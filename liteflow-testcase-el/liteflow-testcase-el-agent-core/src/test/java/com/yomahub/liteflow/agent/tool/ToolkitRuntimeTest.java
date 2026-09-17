package com.yomahub.liteflow.agent.tool;

import io.agentscope.harness.agent.HarnessAgent;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.testsupport.AgentTestContexts;
import com.yomahub.liteflow.agent.testsupport.ScriptedChatModel;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolkitRuntimeTest {

    private static final String AGENT_NAMESPACE =
            "lf-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test
    void defaultToolkitExecutesSeriallyAndInjectsTheCurrentRuntimeContext() {
        SerialProbeTool serial = new SerialProbeTool();
        ContextProbeTool contextProbe = new ContextProbeTool();
        TestComponent component = new TestComponent();
        component.tools = List.of(serial);
        component.toolkitCustomizer = toolkit -> toolkit.registerTool(contextProbe);

        HarnessAgentRuntime runtime = component.runtime(config());
        try {
            LiteFlowAgentContext invocation = AgentTestContexts.liteFlowContext();
            RuntimeContext runtimeContext = AgentTestContexts.runtimeContext(invocation);
            List<ToolResultBlock> results = runtime.agent().getToolkit().callTools(
                    List.of(
                            new ToolUseBlock("serial-1", "serial_probe", Map.of("value", 1),
                                    "{\"value\":1}", Map.of()),
                            new ToolUseBlock("serial-2", "serial_probe", Map.of("value", 2),
                                    "{\"value\":2}", Map.of()),
                            new ToolUseBlock("context-1", "context_probe", Map.of(), "{}", Map.of())),
                    null,
                    runtime.agent(),
                    runtimeContext).block(Duration.ofSeconds(3));

            assertTrue(results != null && results.stream()
                    .allMatch(result -> result.getState() != ToolResultState.ERROR),
                    () -> "registered tools must execute successfully: "
                            + results.stream().map(ToolResultBlock::getOutput).toList());
            assertEquals(1, serial.maxActive.get(),
                    "the LiteFlow Toolkit default must serialize even concurrency-safe tools");
            assertSame(runtimeContext, contextProbe.runtimeContext.get());
            assertSame(invocation, contextProbe.liteFlowContext.get());
        } finally {
            runtime.close();
        }
    }

    @Test
    void explicitShellDisableAndFileToolOptInRetainWorkspaceLeaseSemantics(
            @TempDir Path workspaceRoot) {
        TestComponent disabled = new TestComponent();
        HarnessAgentRuntime disabledRuntime = disabled.runtime(config());
        try {
            assertTrue(disabledRuntime.agent().getToolkit().getToolNames().contains("read_file"));
            assertTrue(disabledRuntime.agent().getToolkit().getToolNames().contains("write_file"));
            assertFalse(disabledRuntime.agent().getToolkit().getToolNames()
                    .contains("execute"));
            assertTrue(disabled.workspaceLeaseRequired());
        } finally {
            disabledRuntime.close();
        }

        AgentConfig enabledConfig = config();
        enabledConfig.getHarness().getLocal().setWorkspaceRoot(workspaceRoot.toString());
        enabledConfig.getHarness().getShell().setMode(ShellMode.WHITELIST);
        TestComponent enabled = new TestComponent();
        enabled.workspaceTools = true;
        enabled.shellTool = true;
        HarnessAgentRuntime enabledRuntime = enabled.runtime(enabledConfig);
        try {
            assertTrue(enabledRuntime.agent().getToolkit().getToolNames().contains("read_file"));
            assertTrue(enabledRuntime.agent().getToolkit().getToolNames().contains("list_files"));
            assertTrue(enabledRuntime.agent().getToolkit().getToolNames().contains("write_file"));
            assertTrue(enabledRuntime.agent().getToolkit().getToolNames().contains("edit_file"));
            assertTrue(enabledRuntime.agent().getToolkit().getToolNames()
                    .contains("execute"));
            assertTrue(enabled.workspaceLeaseRequired());
            assertTrue(Files.isDirectory(workspaceRoot),
                    "the workspace root must be created when built-ins are enabled");
        } finally {
            enabledRuntime.close();
        }
    }

    @Test
    void defaultShellExecutesWithTheDefaultCommandList(@TempDir Path workspaceRoot) {
        TestComponent component = new TestComponent();
        component.shellTool = null; // Exercise the inherited default without overriding it.
        AgentConfig config = config();
        config.getHarness().getLocal().setWorkspaceRoot(workspaceRoot.toString());
        try (HarnessAgentRuntime runtime = component.runtime(config)) {
            assertTrue(runtime.agent().getToolkit().getToolNames().contains("execute"));
            var results = runtime.agent().getToolkit().callTools(
                    List.of(new ToolUseBlock("default-shell", "execute",
                            Map.of("command", "printf default-core-shell"), "{\"command\":\"printf default-core-shell\"}", Map.of())),
                    null, runtime.agent(), AgentTestContexts.runtimeContext(AgentTestContexts.liteFlowContext()))
                    .block(Duration.ofSeconds(5));
            assertTrue(results != null && results.get(0).getOutput().stream()
                    .filter(io.agentscope.core.message.TextBlock.class::isInstance)
                    .map(io.agentscope.core.message.TextBlock.class::cast)
                    .anyMatch(block -> block.getText().contains("default-core-shell")),
                    () -> "Shell result: " + results);
        }
    }

    @Test
    void enabledBuiltInsRequireALocalExecutionRoot() {
        TestComponent component = new TestComponent();
        component.workspaceTools = true;
        component.shellTool = true;

        AgentConfig missingRoot = config();
        missingRoot.getHarness().getLocal().setWorkspaceRoot(null);
        AgentConfigException rootFailure = assertThrows(
                AgentConfigException.class, () -> component.runtime(missingRoot));
        assertTrue(rootFailure.getMessage().contains("root"));
    }

    @Test
    void shellRegistrationRejectsDisabledModeAndBlankWhitelist(@TempDir Path workspaceRoot) {
        assertInvalidShell(workspaceRoot, config -> config.getHarness().getShell().setMode(ShellMode.DISABLED));
        assertInvalidShell(workspaceRoot, config -> config.getHarness().getShell().setWhitelist(null));
        assertInvalidShell(workspaceRoot, config -> config.getHarness().getShell().setWhitelist(List.of()));
        assertInvalidShell(workspaceRoot, config -> config.getHarness().getShell().setWhitelist(List.of("java", " ")));
    }

    @Test
    void customizedAgentCannotSilentlyDropLiteFlowToolkitTools() {
        TestComponent component = new TestComponent();
        ContextProbeTool required = new ContextProbeTool();
        component.tools = List.of(required);
        component.agentCustomizer = builder -> {
            ReActAgent snapshot = builder.build().getDelegate();
            try {
                return HarnessAgent.builder()
                        .name("replacement")
                        .sysPrompt("replacement")
                        .model(component.model)
                        .toolkit(new Toolkit())
                        .defaultSessionId(snapshot.getDefaultSessionId())
                        .stateStore(snapshot.getStateStore())
                        .middlewares(snapshot.getMiddlewares());
            } finally {
                snapshot.close();
            }
        };

        AgentConfigException failure = assertThrows(
                AgentConfigException.class, () -> component.runtime(config()));

        assertTrue(failure.getMessage().contains("provided builder"));
    }

    private static void assertInvalidShell(
            Path root, java.util.function.Consumer<AgentConfig> invalidator) {
        AgentConfig config = config();
        config.getHarness().getLocal().setWorkspaceRoot(root.toString());
        config.getHarness().getShell().setMode(ShellMode.WHITELIST);
        config.getHarness().getShell().setWhitelist(List.of("java"));
        invalidator.accept(config);
        TestComponent component = new TestComponent();
        component.shellTool = true;
        assertThrows(AgentConfigException.class, () -> component.runtime(config));
    }

    private static AgentConfig config() {
        AgentConfig config = new AgentConfig();
        config.getHarness().getLocal().setWorkspaceRoot(java.nio.file.Path.of("target", "harness-tests", java.util.UUID.randomUUID().toString()).toAbsolutePath().toString());
        config.getSessionStore().setJsonWorkspaceRoot(config.getHarness().getLocal().getWorkspaceRoot() + "/records");
        config.getSessionStore().setJsonRoot("target/agent-state");
        config.setApplicationName("toolkit-test");
        config.setExecutionTimeout(Duration.ofSeconds(2));
        return config;
    }

    private static final class TestComponent extends HarnessAgentComponent {
        private final Model model = new ScriptedChatModel("reply");
        private List<Object> tools = List.of();
        private java.util.function.Consumer<Toolkit> toolkitCustomizer = ignored -> { };
        private UnaryOperator<HarnessAgent.Builder> agentCustomizer = UnaryOperator.identity();
        private boolean workspaceTools;
        private Boolean shellTool = false;

        private HarnessAgentRuntime runtime(AgentConfig config) {
            return buildRuntime(new AgentRuntimeBuildContext(
                    config, "toolkit-agent", "agent-key", AGENT_NAMESPACE));
        }

        private boolean workspaceLeaseRequired() {
            return requiresWorkspaceLease();
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
        protected String systemPrompt() {
            return "toolkit test";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "hello";
        }

        @Override
        protected List<Object> tools() {
            return tools;
        }

        @Override
        protected void customizeToolkit(Toolkit toolkit) {
            toolkitCustomizer.accept(toolkit);
        }

        @Override
        protected boolean enableShellTool() {
            return shellTool == null ? super.enableShellTool() : shellTool;
        }

        @Override
        protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            return agentCustomizer.apply(builder);
        }
    }

    private static final class SerialProbeTool {
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maxActive = new AtomicInteger();
        private final CountDownLatch secondEntered = new CountDownLatch(1);

        @Tool(name = "serial_probe", concurrencySafe = true)
        public String probe(@ToolParam(name = "value") int value) throws InterruptedException {
            int now = active.incrementAndGet();
            maxActive.accumulateAndGet(now, Math::max);
            if (value == 1) {
                secondEntered.await(150, TimeUnit.MILLISECONDS);
            } else {
                secondEntered.countDown();
            }
            active.decrementAndGet();
            return Integer.toString(value);
        }
    }

    private static final class ContextProbeTool {
        private final AtomicReference<RuntimeContext> runtimeContext = new AtomicReference<>();
        private final AtomicReference<LiteFlowAgentContext> liteFlowContext = new AtomicReference<>();

        @Tool(name = "context_probe", readOnly = true)
        public String inspect(RuntimeContext current) {
            runtimeContext.set(current);
            liteFlowContext.set(current.get(LiteFlowAgentContext.class));
            return "ok";
        }
    }
}
