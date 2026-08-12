package com.yomahub.liteflow.agent.harness.permission;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.hitl.AgentConfirmationHandler;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolCallState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.Tool;
import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessPermissionHitlTest {

    @TempDir
    Path tempDir;

    private final List<TestComponent> components = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        components.forEach(TestComponent::close);
        LiteflowConfigGetter.clean();
    }

    @Test
    void allowRuleExecutesOnceWithoutCallingHandler() throws Exception {
        configure("allow");
        ScriptedModel model = new ScriptedModel("allow-tool");
        TestComponent component = component(model);
        component.permission = rule(PermissionBehavior.ALLOW);
        component.handler = (event, context) -> Mono.error(new AssertionError("handler called"));

        component.process();

        assertEquals(1, component.tool.executions.get());
        assertEquals(0, component.middleware.resumeCalls.get());
        assertClean(component);
    }

    @Test
    void askApprovalUsesMetadataOnlyResumeAndSameAgentAndRuntimeContext() throws Exception {
        configure("ask-allow");
        ScriptedModel model = new ScriptedModel("ask-tool");
        TestComponent component = component(model);
        component.permission = rule(PermissionBehavior.ASK);
        component.handler = (event, context) -> {
            assertEquals(0, component.tool.executions.get());
            assertSame(component.lastContext.get(), context);
            ToolUseBlock tool = event.getToolCalls().get(0);
            assertEquals("ask-tool", tool.getId());
            assertEquals("execute", tool.getName());
            assertEquals("run-ask-tool", tool.getInput().get("command"));
            return Mono.just(List.of(new ConfirmResult(true, tool)));
        };

        component.process();

        assertEquals(1, component.tool.executions.get());
        assertEquals(1, component.middleware.resumeCalls.get());
        assertEquals(1, component.middleware.agents.stream().distinct().count());
        assertTrue(component.middleware.contexts.stream()
                .allMatch(context -> context == component.middleware.contexts.get(0)));
        assertSame(component.runtime.agent().getDelegate(), component.middleware.agents.get(0));
        assertSame(component.lastRuntimeContext.get(), component.middleware.contexts.get(0));
        assertClean(component);
    }

    @Test
    void askDenialClearsPendingStateWithoutExecutingTool() throws Exception {
        configure("ask-deny");
        TestComponent component = component(new ScriptedModel("denied-tool"));
        component.permission = rule(PermissionBehavior.ASK);
        component.handler = (event, context) -> Mono.just(List.of(
                new ConfirmResult(false, event.getToolCalls().get(0))));

        component.process();

        assertEquals(0, component.tool.executions.get());
        assertEquals(1, component.middleware.resumeCalls.get());
        assertClean(component);
    }

    @Test
    void explicitDenyNeverCallsHandlerOrTool() throws Exception {
        configure("explicit-deny");
        AtomicInteger handlerCalls = new AtomicInteger();
        TestComponent component = component(new ScriptedModel("rule-denied-tool"));
        component.permission = rule(PermissionBehavior.DENY);
        component.handler = (event, context) -> {
            handlerCalls.incrementAndGet();
            return Mono.empty();
        };

        component.process();

        assertEquals(0, handlerCalls.get());
        assertEquals(0, component.tool.executions.get());
        assertEquals(0, component.middleware.resumeCalls.get());
        assertClean(component);
    }

    @Test
    void missingHandlerRunsDenialCleanupThenReturnsPermissionFailure() throws Exception {
        configure("missing-handler");
        TestComponent component = component(new ScriptedModel("missing-handler-tool"));
        component.permission = rule(PermissionBehavior.ASK);

        AgentInvocationException failure =
                assertThrows(AgentInvocationException.class, component::process);

        assertEquals(AgentInvocationErrorType.PERMISSION, failure.getErrorType());
        assertEquals(0, component.tool.executions.get());
        assertEquals(1, component.middleware.resumeCalls.get());
        assertClean(component);
    }

    @Test
    void handlerErrorRunsDenialCleanupAndPreservesPermissionCause() throws Exception {
        configure("handler-error");
        RuntimeException handlerFailure = new RuntimeException("handler failed");
        TestComponent component = component(new ScriptedModel("handler-error-tool"));
        component.permission = rule(PermissionBehavior.ASK);
        component.handler = (event, context) -> Mono.error(handlerFailure);

        AgentInvocationException failure =
                assertThrows(AgentInvocationException.class, component::process);

        assertEquals(AgentInvocationErrorType.PERMISSION, failure.getErrorType());
        assertSame(handlerFailure, failure.getCause());
        assertEquals(0, component.tool.executions.get());
        assertEquals(1, component.middleware.resumeCalls.get());
        assertClean(component);
    }

    @Test
    void multiRoundAskUsesOneFiniteContinuationTransaction() throws Exception {
        configure("multi-round");
        ScriptedModel model = new ScriptedModel("round-one", "round-two");
        TestComponent component = component(model);
        component.permission = rule(PermissionBehavior.ASK);
        AtomicInteger handlerCalls = new AtomicInteger();
        component.handler = (event, context) -> {
            handlerCalls.incrementAndGet();
            return Mono.just(List.of(new ConfirmResult(true, event.getToolCalls().get(0))));
        };

        component.process();

        assertEquals(2, handlerCalls.get());
        assertEquals(2, component.tool.executions.get());
        assertEquals(2, component.middleware.resumeCalls.get());
        assertEquals(1, component.middleware.agents.stream().distinct().count());
        assertTrue(component.middleware.contexts.stream()
                .allMatch(context -> context == component.middleware.contexts.get(0)));
        assertClean(component);
    }

    @Test
    void defaultHarnessPolicyAsksBeforeAnUnruledWriteLikeTool() throws Exception {
        configure("default-policy");
        TestComponent component = component(new ScriptedModel("default-tool"));

        AgentInvocationException failure =
                assertThrows(AgentInvocationException.class, component::process);

        assertEquals(AgentInvocationErrorType.PERMISSION, failure.getErrorType());
        assertEquals(0, component.tool.executions.get());
        assertEquals(PermissionMode.DEFAULT,
                component.runtime.agent().getDelegate()
                        .getAgentState("user", component.lastContext.get().getRuntimeSessionId())
                        .getPermissionContext().getMode());
        assertClean(component);
    }

    @Test
    void customizerCannotReplaceFinalPermissionPolicyWithBypass() throws Exception {
        configure("customizer-bypass");
        TestComponent component = component(new ScriptedModel("bypass-tool"));
        PermissionContextState expected = rule(PermissionBehavior.ASK);
        component.permission = expected;
        component.customizer = builder -> builder.permissionContext(
                PermissionContextState.builder().mode(PermissionMode.BYPASS).build());

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("permission"), failure.getMessage());
        assertEquals(0, component.tool.executions.get());
        assertClean(component);
    }

    @Test
    void configuredBypassPolicyIsRejectedBeforeModelOrToolExecution() throws Exception {
        configure("configured-bypass");
        ScriptedModel model = new ScriptedModel("configured-bypass-tool");
        TestComponent component = component(model);
        component.permission = PermissionContextState.builder()
                .mode(PermissionMode.BYPASS)
                .build();

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("BYPASS"), failure.getMessage());
        assertEquals(0, model.calls.get());
        assertEquals(0, component.tool.executions.get());
        assertClean(component);
    }

    @Test
    void userMiddlewareCannotSwitchTheSessionToBypass() throws Exception {
        configure("middleware-bypass");
        ScriptedModel model = new ScriptedModel("middleware-bypass-tool");
        TestComponent component = component(model);
        component.permission = rule(PermissionBehavior.ALLOW);
        component.beforeRecordingMiddleware = new MiddlewareBase() {
            @Override
            public Flux<AgentEvent> onAgent(
                    Agent agent,
                    RuntimeContext context,
                    AgentInput input,
                    Function<AgentInput, Flux<AgentEvent>> next) {
                ((ReActAgent) agent).setPermissionMode(context, PermissionMode.BYPASS);
                return next.apply(input);
            }
        };

        AgentInvocationException failure =
                assertThrows(AgentInvocationException.class, component::process);

        assertEquals(AgentInvocationErrorType.PERMISSION, failure.getErrorType());
        assertTrue(failure.getMessage().contains("BYPASS"), failure.getMessage());
        assertEquals(0, model.calls.get());
        assertEquals(0, component.tool.executions.get());
        assertClean(component);
    }

    private TestComponent component(ScriptedModel model) {
        Slot slot = new Slot();
        slot.setChainId("permission-chain");
        slot.setConversationId("conversation");
        slot.putRequestId("request");
        TestComponent component = new TestComponent(slot, model);
        component.setNodeId("permission-agent");
        components.add(component);
        return component;
    }

    private void configure(String namespace) throws Exception {
        Path workspace = tempDir.resolve(namespace);
        Files.createDirectories(workspace);
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace(namespace);
        agent.getRuntime().setDefaultUserId("user");
        agent.getRuntime().setTimeout(Duration.ofSeconds(4));
        agent.getHitl().setConfirmationTimeout(Duration.ofSeconds(2));
        agent.getWorkspace().setRoot(workspace.toString());
        agent.getHarness().setFilesystemBackend(HarnessFilesystemBackend.GUARDED_LOCAL);
        agent.getHarness().setTrustedLocal(true);
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(config);
    }

    private static PermissionContextState rule(PermissionBehavior behavior) {
        PermissionRule rule = new PermissionRule("execute", null, behavior, "test");
        PermissionContextState.Builder builder = PermissionContextState.builder();
        switch (behavior) {
            case ALLOW -> builder.addAllowRule("execute", rule);
            case ASK -> builder.addAskRule("execute", rule);
            case DENY -> builder.addDenyRule("execute", rule);
            default -> throw new IllegalArgumentException("unsupported behavior " + behavior);
        }
        return builder.build();
    }

    private static void assertClean(TestComponent component) {
        LiteFlowAgentContext context = component.lastContext.get();
        if (context != null) {
            assertFalse(component.slot.hasAttachment(context.getAttachmentKey()));
        }
    }

    private static final class TestComponent extends HarnessAgentComponent {
        private final Slot slot;
        private final ScriptedModel model;
        private final ExecuteTool tool = new ExecuteTool();
        private final RecordingMiddleware middleware = new RecordingMiddleware();
        private final AtomicReference<LiteFlowAgentContext> lastContext = new AtomicReference<>();
        private final AtomicReference<RuntimeContext> lastRuntimeContext = new AtomicReference<>();
        private PermissionContextState permission;
        private AgentConfirmationHandler handler;
        private MiddlewareBase beforeRecordingMiddleware;
        private Function<HarnessAgent.Builder, HarnessAgent.Builder> customizer = Function.identity();
        private HarnessAgentRuntime runtime;

        private TestComponent(Slot slot, ScriptedModel model) {
            this.slot = slot;
            this.model = model;
        }

        @Override
        public Slot getSlot() {
            return slot;
        }

        @Override
        protected ModelSpec<?> model() {
            throw new AssertionError("buildModel override must be used");
        }

        @Override
        protected Model buildModel() {
            return model;
        }

        @Override
        protected PermissionContextState permissionContext() {
            return permission;
        }

        @Override
        protected AgentConfirmationHandler confirmationHandler() {
            return handler;
        }

        @Override
        protected List<Object> tools() {
            return List.of(tool);
        }

        @Override
        protected List<MiddlewareBase> middlewares() {
            return beforeRecordingMiddleware == null
                    ? List.of(middleware)
                    : List.of(beforeRecordingMiddleware, middleware);
        }

        @Override
        protected void customizeRuntimeContext(
                RuntimeContext.Builder builder, LiteFlowAgentContext context) {
            lastContext.set(context);
            builder.put(InvocationMarker.class, new InvocationMarker(lastRuntimeContext));
        }

        @Override
        protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            HarnessAgent.Builder safe = builder
                    .disableSubagents()
                    .disableCompaction()
                    .disableToolResultEviction()
                    .disableMemoryTools()
                    .disableMemoryHooks()
                    .disableWorkspaceContext()
                    .disableAtPathExpansion()
                    .disableDefaultWorkspaceSkills()
                    .disableDynamicSkills()
                    .disableToolsConfig()
                    .disableFilesystemTools()
                    .disableShellTool();
            return customizer.apply(safe);
        }

        @Override
        protected HarnessAgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
            runtime = super.buildRuntime(buildContext);
            return runtime;
        }

        @Override
        protected String systemPrompt() {
            return "Use the execute tool as instructed.";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "execute";
        }
    }

    private record InvocationMarker(AtomicReference<RuntimeContext> context) {
    }

    private static final class RecordingMiddleware implements MiddlewareBase {
        private final List<Agent> agents = new ArrayList<>();
        private final List<RuntimeContext> contexts = new ArrayList<>();
        private final AtomicInteger resumeCalls = new AtomicInteger();

        @Override
        public Flux<AgentEvent> onAgent(
                Agent agent,
                RuntimeContext context,
                AgentInput input,
                Function<AgentInput, Flux<AgentEvent>> next) {
            agents.add(agent);
            contexts.add(context);
            InvocationMarker marker = context.get(InvocationMarker.class);
            if (marker != null) {
                marker.context().compareAndSet(null, context);
            }
            if (input.msgs().size() == 1
                    && input.msgs().get(0).getMetadata()
                            .containsKey(Msg.METADATA_CONFIRM_RESULTS)) {
                Msg resume = input.msgs().get(0);
                assertTrue(resume.getContent().isEmpty());
                assertEquals(1, resume.getMetadata().size());
                assertFalse(resume.getMetadata()
                        .containsKey(Msg.METADATA_CONFIRM_REQUEST_REPLY_ID));
                resumeCalls.incrementAndGet();
            }
            return next.apply(input);
        }
    }

    private static final class ExecuteTool {
        private final AtomicInteger executions = new AtomicInteger();

        @Tool(name = "execute", concurrencySafe = false)
        public String execute(String command) {
            executions.incrementAndGet();
            return "executed:" + command;
        }
    }

    private static final class ScriptedModel implements Model {
        private final List<String> toolIds;
        private final AtomicInteger calls = new AtomicInteger();

        private ScriptedModel(String... toolIds) {
            this.toolIds = List.of(toolIds);
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            int call = calls.getAndIncrement();
            if (call < toolIds.size()) {
                assertTrue(tools.stream().anyMatch(schema -> "execute".equals(schema.getName())));
                String id = toolIds.get(call);
                ToolUseBlock use = new ToolUseBlock(
                        id,
                        "execute",
                        Map.of("command", "run-" + id),
                        "{\"command\":\"run-" + id + "\"}",
                        Map.of(),
                        ToolCallState.PENDING);
                return Flux.just(ChatResponse.builder()
                        .content(List.<ContentBlock>of(use))
                        .finishReason("tool_calls")
                        .build());
            }
            return Flux.just(ChatResponse.builder()
                    .content(List.of(TextBlock.builder().text("done").build()))
                    .finishReason("stop")
                    .build());
        }

        @Override
        public String getModelName() {
            return "harness-permission-model";
        }
    }
}
