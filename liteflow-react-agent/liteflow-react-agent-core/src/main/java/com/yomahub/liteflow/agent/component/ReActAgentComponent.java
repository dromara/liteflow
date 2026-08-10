package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.guard.AgentInvocationCoordinator;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuard;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuardResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationLease;
import com.yomahub.liteflow.agent.middleware.StateStoreFailureMiddleware;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeHandle;
import com.yomahub.liteflow.agent.runtime.ReActAgentRuntime;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.DefaultAgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.util.ConversationIdGenerator;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Minimal AgentScope 2 ReAct bridge used by the core runtime vertical slice.
 *
 * <p>One component instance owns one lazily built, stateless ReAct runtime. Invocation identity,
 * Slot and RuntimeContext remain call-scoped and are never retained by the runtime.
 */
public abstract class ReActAgentComponent extends NodeComponent implements AutoCloseable {

    public static final String CONVERSATION_ID_REQUEST_KEY = "conversationId";

    public static final String DEFAULT_SYSTEM_PROMPT = """
            请使用用户提问所用的语言回答，除非用户明确要求使用其他语言。
            每次调用工具前，先用一两句话简短说明当前判断和下一步动作，便于日志观察可见推理摘要。
            不要展开隐藏思维链，只输出面向用户和调试日志都可读的简短说明。
            """;

    private final AgentRuntimeHandle<ReActAgentRuntime> runtimeHandle =
            new AgentRuntimeHandle<>();

    protected final AgentConfig agentConfig() {
        AgentConfig config = LiteflowConfigGetter.get().getAgent();
        if (config == null) {
            throw new AgentConfigException(
                    "LiteflowConfig.agent is null; configure liteflow.agent.* or setAgent() before use");
        }
        return config;
    }

    protected abstract ModelSpec<?> model();

    protected Model buildModel() {
        return model().resolve(agentConfig());
    }

    protected abstract String systemPrompt();

    protected final String effectiveSystemPrompt() {
        String customPrompt = systemPrompt();
        if (customPrompt == null || customPrompt.isBlank()) {
            return DEFAULT_SYSTEM_PROMPT.strip();
        }
        return DEFAULT_SYSTEM_PROMPT.strip() + "\n\n" + customPrompt.strip();
    }

    protected abstract String userPrompt();

    protected String resolveUserId() {
        return agentConfig().getRuntime().getDefaultUserId();
    }

    protected String resolveConversationId() {
        Slot slot = getSlot();
        String existing = slot.getConversationId();
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        Object request = slot.getChainReqData(slot.getChainId());
        if (request instanceof Map<?, ?> requestMap) {
            Object configured = requestMap.get(CONVERSATION_ID_REQUEST_KEY);
            if (configured != null && !configured.toString().isBlank()) {
                return configured.toString();
            }
        }
        return ConversationIdGenerator.generate();
    }

    protected String agentKey() {
        String nodeId = getNodeId();
        return nodeId == null || nodeId.isBlank() ? "default" : nodeId;
    }

    protected int maxIterations() {
        return -1;
    }

    protected void handleReply(Msg reply) {
        if (reply != null && reply.getTextContent() != null) {
            getSlot().setResponseData(reply.getTextContent());
        }
    }

    protected AgentStateStoreResolver stateStoreResolver() {
        return new DefaultAgentStateStoreResolver();
    }

    @Override
    public final void process() {
        AgentConfig config = agentConfig();
        validateForExecution(config);

        Slot slot = getSlot();
        String conversationId = resolveConversationId();
        slot.setConversationId(conversationId);
        String currentAgentKey = agentKey();
        AgentInvocationIdentity identity = new InvocationIdentityResolver(
                config.getRuntime().getNamespace())
                .resolve(resolveUserId(), conversationId, currentAgentKey);

        AgentInvocationGuard guard = new AgentInvocationGuardResolver().resolve(config);
        AgentInvocationCoordinator coordinator = new AgentInvocationCoordinator(guard);
        ReActAgentRuntime runtime = null;
        try (AgentInvocationLease ignored = coordinator.acquire(
                identity, false, config.getInvocationGuard().getAcquireTimeout())) {
            AgentRuntimeBuildContext buildContext = new AgentRuntimeBuildContext(
                    config, runtimeAgentName(), currentAgentKey, identity.agentNamespace());
            runtime = runtimeHandle.getOrCreate(() -> buildRuntime(buildContext));
            if (!runtime.stateStore().agentNamespace().equals(identity.agentNamespace())) {
                throw new AgentConfigException(
                        "Agent identity changed after this component runtime was initialized");
            }

            RuntimeContext runtimeContext = RuntimeContext.builder()
                    .userId(identity.userId())
                    .sessionId(identity.runtimeSessionId())
                    .put(Slot.class, slot)
                    .build();
            Msg reply = runtime.agent()
                    .call(List.of(new UserMessage(userPrompt())), runtimeContext)
                    .block();
            handleReply(reply);
        } finally {
            if (runtime != null) {
                runtime.stateStore().clearLoadFailure(
                        identity.userId(), identity.runtimeSessionId());
            }
        }
    }

    private ReActAgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
        ResolvedAgentStateStore resolved = stateStoreResolver().resolve(
                buildContext.agentConfig().getStateStore());
        GuardedNamespacedAgentStateStore namespaced = new GuardedNamespacedAgentStateStore(
                resolved.store(), buildContext.agentNamespace());
        try {
            StateStoreFailureMiddleware failureMiddleware = new StateStoreFailureMiddleware(
                    namespaced,
                    buildContext.agentConfig().getStateStore().getFailurePolicy());
            int iterations = maxIterations() > 0
                    ? maxIterations()
                    : buildContext.agentConfig().getDefaults().getMaxIterations();
            ReActAgent agent = ReActAgent.builder()
                    .name(buildContext.agentName())
                    .sysPrompt(effectiveSystemPrompt())
                    .model(buildModel())
                    .toolkit(new Toolkit())
                    .maxIters(iterations)
                    .stateStore(namespaced)
                    .middleware(failureMiddleware)
                    .build();
            return new ReActAgentRuntime(agent, namespaced, resolved);
        } catch (RuntimeException | Error failure) {
            try {
                namespaced.close();
                resolved.close();
            } catch (RuntimeException | Error closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }

    private String runtimeAgentName() {
        String nodeId = getNodeId();
        return nodeId == null || nodeId.isBlank() ? "liteflow-agent" : nodeId;
    }

    private static void validateForExecution(AgentConfig config) {
        try {
            config.validateForExecution();
        } catch (IllegalStateException failure) {
            throw new AgentConfigException(failure.getMessage(), failure);
        }
        if (config.getRuntime() == null
                || config.getRuntime().getDefaultUserId() == null
                || config.getRuntime().getDefaultUserId().isBlank()) {
            throw new AgentConfigException(
                    "liteflow.agent.runtime.default-user-id must not be blank");
        }
        if (config.getStateStore() == null || config.getStateStore().getType() == null) {
            throw new AgentConfigException(
                    "liteflow.agent.state-store.type must not be null");
        }
        if (config.getStateStore().getFailurePolicy() == null) {
            throw new AgentConfigException(
                    "liteflow.agent.state-store.failure-policy must not be null");
        }
        if (config.getInvocationGuard() == null) {
            throw new AgentConfigException(
                    "liteflow.agent.invocation-guard must not be null");
        }
        Duration acquireTimeout = config.getInvocationGuard().getAcquireTimeout();
        if (acquireTimeout == null || acquireTimeout.isZero() || acquireTimeout.isNegative()) {
            throw new AgentConfigException(
                    "liteflow.agent.invocation-guard.acquire-timeout must be positive");
        }
    }

    @Override
    public final void close() {
        runtimeHandle.close();
    }
}
