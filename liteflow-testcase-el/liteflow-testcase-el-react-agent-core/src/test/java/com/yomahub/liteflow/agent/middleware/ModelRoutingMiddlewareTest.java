package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.testsupport.AgentTestContexts;
import io.agentscope.core.message.Msg;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelRoutingMiddlewareTest {

    @Test
    void selectingDefaultPreservesAgentScopeComposedModelAndEveryOtherInput() {
        TestModel defaultModel = new TestModel("default");
        TestModel fallbackModel = new TestModel("fallback");
        TestModel upstreamComposedModel = new TestModel("upstream-composed");
        LiteFlowAgentContext context = AgentTestContexts.liteFlowContext();
        AtomicInteger routes = new AtomicInteger();
        ModelRoutingMiddleware middleware = new ModelRoutingMiddleware(
                defaultModel,
                List.of(fallbackModel),
                (model, invocation) -> {
                    routes.incrementAndGet();
                    assertSame(context, invocation);
                    return model;
                });
        List<Msg> messages = new ArrayList<>();
        List<ToolSchema> tools = new ArrayList<>();
        GenerateOptions options = GenerateOptions.builder().temperature(0.25).build();
        ModelCallInput input = new ModelCallInput(messages, tools, options, upstreamComposedModel);
        AtomicReference<ModelCallInput> forwarded = new AtomicReference<>();

        middleware.onModelCall(
                        null,
                        AgentTestContexts.runtimeContext(context),
                        input,
                        selected -> {
                            forwarded.set(selected);
                            return Flux.empty();
                        })
                .blockLast();

        assertEquals(1, routes.get());
        assertSame(messages, forwarded.get().messages());
        assertSame(tools, forwarded.get().tools());
        assertSame(options, forwarded.get().options());
        assertSame(upstreamComposedModel, forwarded.get().model());
    }

    @Test
    void selectingAnotherManagedModelReplacesOnlyTheModel() {
        TestModel defaultModel = new TestModel("default");
        TestModel routingModel = new TestModel("routed");
        LiteFlowAgentContext context = AgentTestContexts.liteFlowContext();
        ModelRoutingMiddleware middleware = new ModelRoutingMiddleware(
                defaultModel,
                List.of(routingModel),
                (model, invocation) -> routingModel);
        List<Msg> messages = new ArrayList<>();
        List<ToolSchema> tools = new ArrayList<>();
        GenerateOptions options = GenerateOptions.builder().topP(0.75).build();
        ModelCallInput input = new ModelCallInput(
                messages, tools, options, new TestModel("upstream-composed"));
        AtomicReference<ModelCallInput> forwarded = new AtomicReference<>();

        middleware.onModelCall(
                        null,
                        AgentTestContexts.runtimeContext(context),
                        input,
                        selected -> {
                            forwarded.set(selected);
                            return Flux.empty();
                        })
                .blockLast();

        assertSame(messages, forwarded.get().messages());
        assertSame(tools, forwarded.get().tools());
        assertSame(options, forwarded.get().options());
        assertSame(routingModel, forwarded.get().model());
    }

    @Test
    void nullAndUnmanagedSelectionsFailBeforeNextOrSelectedModelEntry() {
        TestModel defaultModel = new TestModel("default");
        TestModel managed = new TestModel("managed");
        TestModel unmanaged = new TestModel("unmanaged");
        AtomicReference<Model> selection = new AtomicReference<>();
        AtomicInteger nextCalls = new AtomicInteger();
        ModelRoutingMiddleware middleware = new ModelRoutingMiddleware(
                defaultModel,
                List.of(managed),
                (model, invocation) -> selection.get());
        ModelCallInput input = new ModelCallInput(List.of(), List.of(), null, defaultModel);

        AgentConfigException nullFailure = assertThrows(
                AgentConfigException.class,
                () -> middleware.onModelCall(
                                null,
                                AgentTestContexts.runtimeContext(AgentTestContexts.liteFlowContext()),
                                input,
                                ignored -> {
                                    nextCalls.incrementAndGet();
                                    return Flux.empty();
                                })
                        .blockLast());
        assertTrue(nullFailure.getMessage().contains("null"));

        selection.set(unmanaged);
        AgentConfigException unmanagedFailure = assertThrows(
                AgentConfigException.class,
                () -> middleware.onModelCall(
                                null,
                                AgentTestContexts.runtimeContext(AgentTestContexts.liteFlowContext()),
                                input,
                                ignored -> {
                                    nextCalls.incrementAndGet();
                                    return Flux.empty();
                                })
                        .blockLast());
        assertTrue(unmanagedFailure.getMessage().contains("managed"));
        assertEquals(0, nextCalls.get());
        assertEquals(0, unmanaged.calls.get());
    }

    private static final class TestModel implements Model {
        private final String name;
        private final AtomicInteger calls = new AtomicInteger();

        private TestModel(String name) {
            this.name = name;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            calls.incrementAndGet();
            return Flux.empty();
        }

        @Override
        public String getModelName() {
            return name;
        }
    }
}
