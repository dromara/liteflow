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
import io.agentscope.core.event.RequireUserConfirmEvent;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

    @ParameterizedTest
    @EnumSource(HandlerFailure.class)
    void nullEmptyAndTimedOutHandlerResultsFailClosedAfterDenialCleanup(
            HandlerFailure kind) throws Exception {
        configure("handler-" + kind.name().toLowerCase(), Duration.ofMillis(40),
                Duration.ofSeconds(4));
        TestComponent component = component(new ScriptedModel("invalid-handler-tool"));
        component.permission = rule(PermissionBehavior.ASK);
        component.handler = switch (kind) {
            case NULL -> (event, context) -> null;
            case EMPTY -> (event, context) -> Mono.empty();
            case TIMEOUT -> (event, context) -> Mono.never();
        };

        AgentInvocationException failure =
                assertThrows(AgentInvocationException.class, component::process);

        assertEquals(kind == HandlerFailure.TIMEOUT
                        ? AgentInvocationErrorType.TIMEOUT
                        : AgentInvocationErrorType.PERMISSION,
                failure.getErrorType());
        assertEquals(0, component.tool.executions.get());
        assertEquals(1, component.middleware.resumeCalls.get());
        assertClean(component);
    }

    @ParameterizedTest
    @EnumSource(InvalidConfirmation.class)
    void malformedConfirmationResultsFailClosedWithoutExecutingTools(
            InvalidConfirmation kind) throws Exception {
        configure("invalid-confirmation-" + kind.name().toLowerCase());
        TestComponent component = component(new ScriptedModel("pending-tool"));
        component.permission = rule(PermissionBehavior.ASK);
        component.handler = (event, context) -> {
            ToolUseBlock pending = event.getToolCalls().get(0);
            return Mono.just(switch (kind) {
                case EMPTY -> List.of();
                case DUPLICATE -> List.of(
                        new ConfirmResult(true, pending),
                        new ConfirmResult(false, pending));
                case UNKNOWN_ID -> List.of(new ConfirmResult(true,
                        toolUse("unknown", pending.getName(), pending.getInput())));
                case FIELD_MISMATCH -> List.of(new ConfirmResult(true,
                        toolUse(pending.getId(), "renamed", pending.getInput())));
            });
        };

        AgentInvocationException failure =
                assertThrows(AgentInvocationException.class, component::process);

        assertEquals(AgentInvocationErrorType.PERMISSION, failure.getErrorType());
        assertEquals(0, component.tool.executions.get());
        assertEquals(1, component.middleware.resumeCalls.get());
        assertClean(component);
    }

    @Test
    void mismatchedConfirmationReplyIdFailsClosedAcrossPublicHarnessCall() throws Exception {
        configure("reply-id-mismatch");
        TestComponent component = component(new ScriptedModel("reply-id-tool"));
        component.permission = rule(PermissionBehavior.ASK);
        component.handler = (event, context) -> Mono.just(List.of(
                new ConfirmResult(true, event.getToolCalls().get(0))));
        component.beforeRecordingMiddleware = new MiddlewareBase() {
            @Override
            public Flux<AgentEvent> onAgent(
                    Agent agent,
                    RuntimeContext context,
                    AgentInput input,
                    Function<AgentInput, Flux<AgentEvent>> next) {
                return next.apply(input).doOnNext(event -> {
                    if (event instanceof RequireUserConfirmEvent confirmation) {
                        context.get(LiteFlowAgentContext.class).recordConfirmationEvent(
                                new RequireUserConfirmEvent(
                                        "wrong-reply-id", confirmation.getToolCalls()));
                    }
                });
            }
        };

        AgentInvocationException failure =
                assertThrows(AgentInvocationException.class, component::process);

        assertEquals(AgentInvocationErrorType.PERMISSION, failure.getErrorType());
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
    void denialCleanupCanBeFollowedByAnotherAskInTheSameTransaction() throws Exception {
        configure("deny-then-ask");
        TestComponent component = component(new ScriptedModel("denied-first", "approved-second"));
        component.permission = rule(PermissionBehavior.ASK);
        AtomicInteger decisions = new AtomicInteger();
        component.handler = (event, context) -> Mono.just(List.of(new ConfirmResult(
                decisions.getAndIncrement() != 0, event.getToolCalls().get(0))));

        component.process();

        assertEquals(2, decisions.get());
        assertEquals(1, component.tool.executions.get());
        assertEquals(2, component.middleware.resumeCalls.get());
        assertClean(component);
    }

    @Test
    void runtimeTimeoutCancelsHarnessCallAndCleansInvocationAttachment() throws Exception {
        configure("runtime-timeout", Duration.ofSeconds(2), Duration.ofMillis(40));
        ScriptedModel model = ScriptedModel.never();
        TestComponent component = component(model);
        component.permission = rule(PermissionBehavior.ALLOW);

        AgentInvocationException failure =
                assertThrows(AgentInvocationException.class, component::process);

        assertEquals(AgentInvocationErrorType.TIMEOUT, failure.getErrorType());
        assertTrue(model.cancelled.await(1, TimeUnit.SECONDS));
        assertEquals(0, component.tool.executions.get());
        assertClean(component);
    }

    @Test
    void callerCancellationCleansHarnessInvocationAttachment() throws Exception {
        configure("caller-cancel");
        ScriptedModel model = ScriptedModel.never();
        TestComponent component = component(model);
        component.permission = rule(PermissionBehavior.ALLOW);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> invocation = executor.submit(() -> {
                try {
                    component.process();
                }
                catch (Exception failure) {
                    throw new RuntimeException(failure);
                }
            });
            assertTrue(model.subscribed.await(1, TimeUnit.SECONDS));

            assertTrue(invocation.cancel(true));

            assertTrue(model.cancelled.await(1, TimeUnit.SECONDS));
        }
        finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        }
        assertClean(component);
    }

    @Test
    void defaultHarnessPolicyBypassesConfirmation() throws Exception {
        configure("default-policy");
        TestComponent component = component(new ScriptedModel("default-tool"));

        component.process();

        assertEquals(1, component.tool.executions.get());
        assertEquals(0, component.middleware.resumeCalls.get());
        assertEquals(PermissionMode.BYPASS,
                component.runtime.agent().getDelegate()
                        .getAgentState(null, component.lastContext.get().getRuntimeSessionId())
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
    void configuredBypassPolicyExecutesWithoutConfirmation() throws Exception {
        configure("configured-bypass");
        ScriptedModel model = new ScriptedModel("configured-bypass-tool");
        TestComponent component = component(model);
        component.permission = PermissionContextState.builder()
                .mode(PermissionMode.BYPASS)
                .build();

        component.process();

        assertEquals(2, model.calls.get());
        assertEquals(1, component.tool.executions.get());
        assertEquals(0, component.middleware.resumeCalls.get());
        assertClean(component);
    }

    @Test
    void defaultBypassReplacesAPersistedLegacyPermissionContext() throws Exception {
        configure("legacy-permission");
        ScriptedModel model = new ScriptedModel();
        TestComponent component = component(model);

        component.process();
        component.runtime.agent().getDelegate().replacePermissionContext(
                null, component.lastContext.get().getRuntimeSessionId(),
                rule(PermissionBehavior.ASK));
        model.script("legacy-tool");

        component.process();

        assertEquals(1, component.tool.executions.get());
        assertEquals(0, component.middleware.resumeCalls.get());
        assertEquals(PermissionMode.BYPASS,
                component.runtime.agent().getDelegate()
                        .getAgentState(null, component.lastContext.get().getRuntimeSessionId())
                        .getPermissionContext().getMode());
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

    @Test
    void customizerMiddlewareCannotSwitchPermissionToBypassInsideMandatoryGuard() throws Exception {
        configure("customizer-middleware-bypass");
        ScriptedModel model = new ScriptedModel("customizer-middleware-bypass-tool");
        TestComponent component = component(model);
        component.permission = rule(PermissionBehavior.ASK);
        component.customizer = builder -> builder.middleware(new MiddlewareBase() {
            @Override
            public int order() {
                return Integer.MIN_VALUE;
            }

            @Override
            public Flux<AgentEvent> onAgent(
                    Agent agent,
                    RuntimeContext context,
                    AgentInput input,
                    Function<AgentInput, Flux<AgentEvent>> next) {
                ((ReActAgent) agent).setPermissionMode(context, PermissionMode.BYPASS);
                return next.apply(input);
            }
        });

        AgentInvocationException failure =
                assertThrows(AgentInvocationException.class, component::process);

        assertEquals(AgentInvocationErrorType.PERMISSION, failure.getErrorType());
        assertTrue(failure.getMessage().contains("BYPASS"), failure.getMessage());
        assertEquals(0, model.calls.get());
        assertEquals(0, component.tool.executions.get());
        assertEquals(PermissionMode.DEFAULT,
                component.runtime.agent().getDelegate()
                        .getAgentState(null, component.lastContext.get().getRuntimeSessionId())
                        .getPermissionContext().getMode());

        AgentInvocationException secondFailure =
                assertThrows(AgentInvocationException.class, component::process);
        assertEquals(AgentInvocationErrorType.PERMISSION, secondFailure.getErrorType());
        assertEquals(0, model.calls.get());
        assertEquals(PermissionMode.DEFAULT,
                component.runtime.agent().getDelegate()
                        .getAgentState(null, component.lastContext.get().getRuntimeSessionId())
                        .getPermissionContext().getMode());
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
        configure(namespace, Duration.ofSeconds(2), Duration.ofSeconds(4));
    }

    private void configure(
            String namespace, Duration confirmationTimeout, Duration runtimeTimeout)
            throws Exception {
        Path workspace = tempDir.resolve(namespace);
        Files.createDirectories(workspace);
        AgentConfig agent = new AgentConfig();
        agent.setApplicationName(namespace);
        agent.setExecutionTimeout(runtimeTimeout);
        agent.getHitl().setConfirmationTimeout(confirmationTimeout);
        agent.getHarness().getLocal().setWorkspaceRoot(workspace.toString());
        agent.getSessionStore().setJsonWorkspaceRoot(workspace.toString());
        agent.getHarness().setFilesystemBackend(HarnessFilesystemBackend.GUARDED_LOCAL);
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

    private static ToolUseBlock toolUse(String id, String name, Map<String, Object> input) {
        return new ToolUseBlock(id, name, input, null, Map.of(), ToolCallState.ASKING);
    }

    private enum HandlerFailure {
        NULL,
        EMPTY,
        TIMEOUT
    }

    private enum InvalidConfirmation {
        EMPTY,
        DUPLICATE,
        UNKNOWN_ID,
        FIELD_MISMATCH
    }

    private static void assertClean(TestComponent component) {
        LiteFlowAgentContext context = component.lastContext.get();
        if (context != null) {
            assertFalse(component.slot.hasAttachment(context.getAttachmentKey()));
        }
    }

    private static final class TestComponent extends HarnessAgentComponent {

        // 断言依赖状态实例同一性，测试内用进程内状态存储。
        @Override protected com.yomahub.liteflow.agent.state.AgentStateStoreResolver stateStoreResolver() {
            return config -> new com.yomahub.liteflow.agent.state.ResolvedAgentStateStore(
                    new io.agentscope.core.state.InMemoryAgentStateStore(), true);
        }
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
        private List<String> toolIds;
        private final AtomicInteger calls = new AtomicInteger();
        private final boolean never;
        private final CountDownLatch subscribed = new CountDownLatch(1);
        private final CountDownLatch cancelled = new CountDownLatch(1);

        private ScriptedModel(String... toolIds) {
            this.toolIds = List.of(toolIds);
            this.never = false;
        }

        private ScriptedModel(boolean never) {
            this.toolIds = List.of();
            this.never = never;
        }

        private static ScriptedModel never() {
            return new ScriptedModel(true);
        }

        private void script(String... toolIds) {
            this.toolIds = List.of(toolIds);
            calls.set(0);
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            int call = calls.getAndIncrement();
            if (never) {
                return Flux.<ChatResponse>never()
                        .doOnSubscribe(ignored -> subscribed.countDown())
                        .doOnCancel(cancelled::countDown);
            }
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
