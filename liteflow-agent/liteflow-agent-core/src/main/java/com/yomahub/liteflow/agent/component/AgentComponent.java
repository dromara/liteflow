package com.yomahub.liteflow.agent.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.hitl.AgentCallTarget;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.middleware.AgentMiddlewareOrder;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.PreparedAgentResources;
import com.yomahub.liteflow.agent.runtime.AgentRuntime;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.middleware.MiddlewareBase;
import reactor.core.publisher.Mono;

import java.util.List;

/** AgentScope 2 implementation of the shared LiteFlow invocation template. */
public abstract class AgentComponent
        extends AbstractAgentScopeComponent<AgentRuntime> {

    protected ReActAgent.Builder customizeAgent(ReActAgent.Builder builder) {
        return builder;
    }

    @Override
    protected AgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
        PreparedAgentResources prepared = prepareAgentResources(buildContext, true, false);
        ReActAgent agent = null;
        try {
            ReActAgent.Builder builder = ReActAgent.builder()
                    .name(buildContext.agentName())
                    .sysPrompt(effectiveSystemPrompt())
                    .model(prepared.defaultModel())
                    .toolkit(prepared.toolkit())
                    .maxIters(prepared.maxIterations())
                    .modelExecutionConfig(prepared.modelExecutionConfig())
                    .toolExecutionConfig(prepared.toolExecutionConfig())
                    .maxRetries(prepared.maxRetries())
                    .fallbackModel(prepared.fallbackModel())
                    .permissionContext(prepared.permissionContext())
                    .stopOnReject(prepared.stopOnReject())
                    .defaultSessionId(buildContext.agentNamespace())
                    .stateStore(prepared.ownership().stateStore());
            addMiddlewaresBeforeUser(builder, prepared);
            for (var repository : prepared.skillRepositories()) {
                builder.skillRepository(repository);
            }
            builder.skillFilter(prepared.skillFilter())
                    .dynamicSkillsEnabled(false)
                    .skillCodeExecutionEnabled(false);
            for (MiddlewareBase middleware : prepared.userMiddlewares()) {
                builder.middleware(AgentMiddlewareOrder.user(middleware));
            }
            addMiddlewaresAfterUser(builder, prepared);

            ReActAgent.Builder customized = customizeAgent(builder);
            if (customized == null) {
                throw new AgentConfigException("customizeAgent must not return null");
            }
            agent = customized.build();
            validateCustomizedRuntime(
                    "customizeAgent",
                    agent.getStateStore(),
                    agent.getMiddlewares(),
                    agent.getModel(),
                    agent.getModelConfig().fallbackModel(),
                    agent.getToolkit(),
                    prepared);
            prepared.routingMiddleware().finalizeDefaultModel(agent.getModel());
            return new AgentRuntime(agent, prepared.ownership());
        }
        catch (RuntimeException | Error failure) {
            prepared.ownership().rollback(failure, agent, List.of());
            throw failure;
        }
    }

    @Override
    protected Mono<Msg> invokeRuntime(
            AgentRuntime runtime,
            List<Msg> input,
            AgentOutputSpec output,
            RuntimeContext runtimeContext,
            LiteFlowAgentContext liteflowContext) {
        ReActAgent agent = runtime.agent();
        return invokeCallTarget(
                new AgentCallTarget() {
                    @Override
                    public Mono<Msg> call(List<Msg> messages, RuntimeContext context) {
                        return agent.call(messages, context);
                    }

                    @Override
                    public Mono<Msg> call(
                            List<Msg> messages, Class<?> type, RuntimeContext context) {
                        return agent.call(messages, type, context);
                    }

                    @Override
                    public Mono<Msg> call(
                            List<Msg> messages, JsonNode schema, RuntimeContext context) {
                        return agent.call(messages, schema, context);
                    }
                },
                runtime.stateStore(),
                input,
                output,
                runtimeContext,
                liteflowContext);
    }

    private static void addMiddlewaresBeforeUser(
            ReActAgent.Builder builder, PreparedAgentResources prepared) {
        List<MiddlewareBase> middlewares = prepared.coreMiddlewares();
        for (int index = 0; index < middlewares.size() - 2; index++) {
            builder.middleware(middlewares.get(index));
        }
    }

    private static void addMiddlewaresAfterUser(
            ReActAgent.Builder builder, PreparedAgentResources prepared) {
        List<MiddlewareBase> middlewares = prepared.coreMiddlewares();
        builder.middleware(middlewares.get(middlewares.size() - 2));
        builder.middleware(middlewares.get(middlewares.size() - 1));
    }
}
