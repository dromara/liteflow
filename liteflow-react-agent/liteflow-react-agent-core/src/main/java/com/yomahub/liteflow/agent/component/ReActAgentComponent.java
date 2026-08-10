package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.middleware.StateStoreFailureMiddleware;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.ReActAgentRuntime;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.DefaultAgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Mono;

import java.util.List;

/** AgentScope 2 ReAct specialization of the shared LiteFlow invocation template. */
public abstract class ReActAgentComponent extends AbstractAgentComponent<ReActAgentRuntime> {

    public static final String DEFAULT_SYSTEM_PROMPT = """
            请使用用户提问所用的语言回答，除非用户明确要求使用其他语言。
            每次调用工具前，先用一两句话简短说明当前判断和下一步动作，便于日志观察可见推理摘要。
            不要展开隐藏思维链，只输出面向用户和调试日志都可读的简短说明。
            """;

    protected abstract ModelSpec<?> model();

    protected Model buildModel() {
        return model().resolve(agentConfig());
    }

    protected final String effectiveSystemPrompt() {
        String customPrompt = systemPrompt();
        if (customPrompt == null || customPrompt.isBlank()) {
            return DEFAULT_SYSTEM_PROMPT.strip();
        }
        return DEFAULT_SYSTEM_PROMPT.strip() + "\n\n" + customPrompt.strip();
    }

    protected int maxIterations() {
        return -1;
    }

    protected AgentStateStoreResolver stateStoreResolver() {
        return new DefaultAgentStateStoreResolver();
    }

    @Override
    protected ReActAgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
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
                    .middleware(new InvocationSystemPromptMiddleware(this))
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

    @Override
    protected Mono<Msg> invokeRuntime(
            ReActAgentRuntime runtime,
            List<Msg> input,
            AgentOutputSpec output,
            RuntimeContext runtimeContext,
            LiteFlowAgentContext liteflowContext) {
        if (!runtime.stateStore().agentNamespace().equals(liteflowContext.getAgentNamespace())) {
            return Mono.error(new AgentConfigException(
                    "Agent identity changed after this component runtime was initialized"));
        }
        return switch (output.kind()) {
            case TEXT -> runtime.agent().call(input, runtimeContext);
            case JAVA_TYPE -> runtime.agent().call(input, output.javaType(), runtimeContext);
            case JSON_SCHEMA -> runtime.agent().call(input, output.jsonSchema(), runtimeContext);
        };
    }

    private static final class InvocationSystemPromptMiddleware implements MiddlewareBase {

        private final ReActAgentComponent component;

        private InvocationSystemPromptMiddleware(ReActAgentComponent component) {
            this.component = component;
        }

        @Override
        public Mono<String> onSystemPrompt(
                Agent agent, RuntimeContext runtimeContext, String currentPrompt) {
            if (runtimeContext == null) {
                return Mono.error(new AgentConfigException(
                        "RuntimeContext is required for dynamic system prompt transformation"));
            }
            LiteFlowAgentContext liteflowContext =
                    runtimeContext.get(LiteFlowAgentContext.class);
            if (liteflowContext == null) {
                return Mono.error(new AgentConfigException(
                        "LiteFlowAgentContext is required for dynamic system prompt transformation"));
            }
            return Mono.defer(() -> {
                Mono<String> transformed = component.transformSystemPrompt(
                        currentPrompt, liteflowContext);
                if (transformed == null) {
                    return Mono.error(new AgentConfigException(
                            "transformSystemPrompt must not return null"));
                }
                return transformed.switchIfEmpty(Mono.error(new AgentConfigException(
                        "transformSystemPrompt must not produce null")));
            });
        }
    }
}
