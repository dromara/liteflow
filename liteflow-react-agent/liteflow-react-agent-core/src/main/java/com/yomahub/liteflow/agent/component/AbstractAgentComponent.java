package com.yomahub.liteflow.agent.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.guard.AgentInvocationCoordinator;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuard;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuardResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationLease;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.message.AgentReplyHandler;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeHandle;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.util.ConversationIdGenerator;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Shared lifecycle template for LiteFlow components backed by an Agent runtime. */
public abstract class AbstractAgentComponent<R extends AutoCloseable>
        extends NodeComponent implements AutoCloseable {

    public static final String CONVERSATION_ID_REQUEST_KEY = "conversationId";

    private final AgentRuntimeHandle<R> runtimeHandle = new AgentRuntimeHandle<>();
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock(true);

    protected abstract R buildRuntime(AgentRuntimeBuildContext buildContext);

    protected abstract Mono<Msg> invokeRuntime(
            R runtime,
            List<Msg> input,
            AgentOutputSpec output,
            RuntimeContext runtimeContext,
            LiteFlowAgentContext liteflowContext);

    protected abstract String systemPrompt();

    protected abstract String userPrompt(LiteFlowAgentContext context);

    protected final AgentConfig agentConfig() {
        AgentConfig config = LiteflowConfigGetter.get().getAgent();
        if (config == null) {
            throw new AgentConfigException(
                    "LiteflowConfig.agent is null; configure liteflow.agent.* or setAgent() before use");
        }
        return config;
    }

    protected String resolveUserId(Slot slot) {
        return agentConfig().getRuntime().getDefaultUserId();
    }

    protected String resolveConversationId(Slot slot) {
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

    protected Mono<String> transformSystemPrompt(
            String currentPrompt, LiteFlowAgentContext context) {
        return Mono.just(currentPrompt);
    }

    protected Class<?> structuredOutputType() {
        return null;
    }

    protected JsonNode structuredOutputSchema() {
        return null;
    }

    protected void customizeRuntimeContext(
            RuntimeContext.Builder builder, LiteFlowAgentContext context) {
    }

    protected void handleReply(Msg reply, LiteFlowAgentContext context) {
        AgentReplyHandler.handle(reply, context.getOutputSpec(), context.getSlot());
    }

    protected boolean requiresWorkspaceLease() {
        return false;
    }

    @Override
    public final void process() throws Exception {
        Lock invocationLease = lifecycleLock.readLock();
        invocationLease.lock();
        try {
            if (runtimeHandle.isClosed()) {
                throw new IllegalStateException("Agent component is closed");
            }
            processWithRuntime();
        } finally {
            invocationLease.unlock();
        }
    }

    private void processWithRuntime() throws Exception {
        AgentConfig config = agentConfig();
        validateForExecution(config);
        AgentOutputSpec output = resolveOutputSpec();
        Duration runtimeTimeout = config.getRuntime().getTimeout();

        Slot slot = getSlot();
        String conversationId = resolveConversationId(slot);
        slot.setConversationId(conversationId);
        String currentAgentKey = agentKey();
        AgentInvocationIdentity identity = new InvocationIdentityResolver(
                config.getRuntime().getNamespace())
                .resolve(resolveUserId(slot), conversationId, currentAgentKey);

        AgentInvocationGuard guard = new AgentInvocationGuardResolver().resolve(config);
        AgentInvocationCoordinator coordinator = new AgentInvocationCoordinator(guard);
        try (AgentInvocationLease ignored = coordinator.acquire(
                identity,
                requiresWorkspaceLease(),
                config.getInvocationGuard().getAcquireTimeout())) {
            AgentRuntimeBuildContext buildContext = new AgentRuntimeBuildContext(
                    config, runtimeAgentName(), currentAgentKey, identity.agentNamespace());
            R runtime = runtimeHandle.getOrCreate(() -> buildRuntime(buildContext));

            ensureRequestId(slot);
            String requestId = slot.getRequestId();
            String attachmentKey = LiteFlowAgentContext.SLOT_ATTACHMENT_PREFIX + UUID.randomUUID();
            LiteFlowAgentContext context = new LiteFlowAgentContext(
                    identity,
                    slot,
                    slot.getChainId(),
                    getNodeId(),
                    requestId,
                    requestId,
                    Instant.now().plus(runtimeTimeout),
                    output,
                    attachmentKey);
            slot.setAttachment(attachmentKey, context);
            try {
                RuntimeContext.Builder runtimeContextBuilder = RuntimeContext.builder()
                        .userId(identity.userId())
                        .sessionId(identity.runtimeSessionId())
                        .put(LiteFlowAgentContext.class, context)
                        .put(Slot.class, slot);
                customizeRuntimeContext(runtimeContextBuilder, context);
                RuntimeContext runtimeContext = runtimeContextBuilder.build();

                String prompt = userPrompt(context);
                if (prompt == null) {
                    throw new AgentConfigException("userPrompt must not return null");
                }
                Mono<Msg> invocation = invokeRuntime(
                        runtime,
                        List.of(new UserMessage(prompt)),
                        output,
                        runtimeContext,
                        context);
                if (invocation == null) {
                    throw new AgentConfigException("invokeRuntime must not return null");
                }
                Msg reply = invocation
                        .doOnCancel(context::cancel)
                        .timeout(runtimeTimeout,
                                Mono.error(new RuntimeDeadlineExceededException(runtimeTimeout)))
                        .onErrorMap(RuntimeDeadlineExceededException.class, failure ->
                                new AgentInvocationException(
                                        AgentInvocationErrorType.TIMEOUT,
                                        failure.getMessage(),
                                        failure))
                        .block();
                handleReply(reply, context);
            } finally {
                slot.removeAttachment(attachmentKey, context);
            }
        }
    }

    @Override
    public final void close() {
        if (lifecycleLock.getReadHoldCount() > 0) {
            throw new IllegalStateException(
                    "Cannot close agent component from an active invocation");
        }
        Lock closeLease = lifecycleLock.writeLock();
        closeLease.lock();
        try {
            runtimeHandle.close();
        } finally {
            closeLease.unlock();
        }
    }

    private AgentOutputSpec resolveOutputSpec() {
        Class<?> outputType = structuredOutputType();
        JsonNode outputSchema = structuredOutputSchema();
        if (outputType != null && outputSchema != null) {
            throw new AgentConfigException(
                    "structuredOutputType and structuredOutputSchema are mutually exclusive");
        }
        if (outputType != null) {
            return AgentOutputSpec.javaType(outputType);
        }
        if (outputSchema != null) {
            return AgentOutputSpec.jsonSchema(outputSchema);
        }
        return AgentOutputSpec.text();
    }

    private String runtimeAgentName() {
        String nodeId = getNodeId();
        return nodeId == null || nodeId.isBlank() ? "liteflow-agent" : nodeId;
    }

    private static void ensureRequestId(Slot slot) {
        if (slot.getRequestId() == null || slot.getRequestId().isBlank()) {
            slot.putRequestId(UUID.randomUUID().toString());
        }
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
        Duration runtimeTimeout = config.getRuntime().getTimeout();
        if (runtimeTimeout == null || runtimeTimeout.isZero() || runtimeTimeout.isNegative()) {
            throw new AgentConfigException(
                    "liteflow.agent.runtime.timeout must be positive");
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

    private static final class RuntimeDeadlineExceededException extends TimeoutException {

        private RuntimeDeadlineExceededException(Duration runtimeTimeout) {
            super("Agent invocation exceeded runtime timeout " + runtimeTimeout);
        }
    }
}
