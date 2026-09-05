package com.yomahub.liteflow.agent.a2a;

import com.yomahub.liteflow.agent.component.AbstractAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import io.agentscope.core.a2a.agent.A2aAgentConfig;
import io.agentscope.core.a2a.agent.card.AgentCardResolver;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** LiteFlow node that invokes a remote A2A agent through an isolated per-call instance. */
public abstract class A2aAgentComponent extends AbstractAgentComponent<A2aClientRuntime> {

    public static final String USER_ID_METADATA = "liteflow.userId";
    public static final String CONVERSATION_ID_METADATA = "liteflow.conversationId";
    public static final String AGENT_KEY_METADATA = "liteflow.agentKey";
    public static final String TRACE_ID_METADATA = "liteflow.traceId";

    protected abstract String remoteAgentName();

    protected abstract AgentCardResolver agentCardResolver();

    protected A2aAgentConfig a2aAgentConfig() {
        return A2aAgentConfig.builder().build();
    }

    protected A2aClientRuntimeFactory a2aClientRuntimeFactory() {
        return A2aClientRuntimeFactory.defaultFactory();
    }

    @Override
    protected final A2aClientRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
        Objects.requireNonNull(buildContext, "buildContext");
        AgentCardResolver resolver = Objects.requireNonNull(
                agentCardResolver(), "agentCardResolver must not return null");
        A2aAgentConfig config = Objects.requireNonNull(
                a2aAgentConfig(), "a2aAgentConfig must not return null");
        A2aClientRuntimeFactory factory = Objects.requireNonNull(
                a2aClientRuntimeFactory(), "a2aClientRuntimeFactory must not return null");
        A2aClientRuntime delegate = Objects.requireNonNull(
                factory.create(resolver, config), "A2A runtime factory returned null");
        return new BoundRuntime(resolver, config, delegate);
    }

    @Override
    protected final Mono<Msg> invokeRuntime(
            A2aClientRuntime runtime,
            List<Msg> input,
            AgentOutputSpec output,
            RuntimeContext runtimeContext,
            LiteFlowAgentContext liteflowContext) {
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(runtimeContext, "runtimeContext");
        Objects.requireNonNull(liteflowContext, "liteflowContext");
        if (output.kind() != AgentOutputSpec.Kind.TEXT) {
            throw new AgentConfigException("A2A client supports TEXT output only");
        }
        if (input == null || input.size() != 1 || !(input.get(0) instanceof UserMessage source)) {
            throw new AgentConfigException("A2A client requires exactly one UserMessage");
        }
        if (!(runtime instanceof BoundRuntime boundRuntime)) {
            throw new AgentConfigException("A2A runtime is missing its immutable binding");
        }

        Duration remaining = Duration.between(Instant.now(), liteflowContext.getDeadline());
        if (remaining.isZero() || remaining.isNegative()) {
            return Mono.error(new AgentInvocationException(
                    AgentInvocationErrorType.TIMEOUT, "A2A invocation deadline has expired"));
        }
        UserMessage message = UserMessage.builder()
                .content(source.getContent())
                .metadata(Map.of(
                        USER_ID_METADATA, runtimeContext.getUserId(),
                        CONVERSATION_ID_METADATA, liteflowContext.getConversationId(),
                        AGENT_KEY_METADATA, liteflowContext.getAgentKey(),
                        TRACE_ID_METADATA, liteflowContext.getTraceId()))
                .build();
        return boundRuntime.call(
                requireRemoteAgentName(), message, remaining, liteflowContext);
    }

    @Override
    protected final String systemPrompt() {
        return "";
    }

    private String requireRemoteAgentName() {
        String name = remoteAgentName();
        if (name == null || name.isBlank()) {
            throw new AgentConfigException("remoteAgentName must not be blank");
        }
        return name;
    }

    private record BoundRuntime(
            AgentCardResolver resolver,
            A2aAgentConfig config,
            A2aClientRuntime delegate) implements A2aClientRuntime {

        private Mono<Msg> call(
                String remoteAgentName,
                UserMessage message,
                Duration timeout,
                LiteFlowAgentContext context) {
            return delegate.call(new A2aClientRequest(
                    remoteAgentName, resolver, config, message, timeout, context));
        }

        @Override
        public Mono<Msg> call(A2aClientRequest request) {
            return delegate.call(request);
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
