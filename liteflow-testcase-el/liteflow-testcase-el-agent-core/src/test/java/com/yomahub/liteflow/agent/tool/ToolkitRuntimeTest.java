package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.agent.component.AgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.AgentRuntime;
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

        AgentRuntime runtime = component.runtime(config());
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
    void agentScopeBuiltInsAreDisabledByDefaultAndRequireAWorkspaceLeaseWhenEnabled(
            @TempDir Path workspaceRoot) {
        TestComponent disabled = new TestComponent();
        AgentRuntime disabledRuntime = disabled.runtime(config());
        try {
            assertFalse(disabledRuntime.agent().getToolkit().getToolNames().contains("view_text_file"));
            assertFalse(disabledRuntime.agent().getToolkit().getToolNames().contains("write_text_file"));
            assertFalse(disabledRuntime.agent().getToolkit().getToolNames()
                    .contains("execute_shell_command"));
            assertFalse(disabled.workspaceLeaseRequired());
        } finally {
            disabledRuntime.close();
        }

        AgentConfig enabledConfig = config();
        enabledConfig.getWorkspace().setRoot(workspaceRoot.toString());
        enabledConfig.getWorkspace().setTrustedLocal(true);
        enabledConfig.getShell().setMode(ShellMode.WHITELIST);
        TestComponent enabled = new TestComponent();
        enabled.workspaceTools = true;
        enabled.shellTool = true;
        AgentRuntime enabledRuntime = enabled.runtime(enabledConfig);
        try {
            assertTrue(enabledRuntime.agent().getToolkit().getToolNames().contains("view_text_file"));
            assertTrue(enabledRuntime.agent().getToolkit().getToolNames().contains("list_directory"));
            assertTrue(enabledRuntime.agent().getToolkit().getToolNames().contains("write_text_file"));
            assertTrue(enabledRuntime.agent().getToolkit().getToolNames().contains("insert_text_file"));
            assertTrue(enabledRuntime.agent().getToolkit().getToolNames()
                    .contains("execute_shell_command"));
            assertTrue(enabled.workspaceLeaseRequired());
            assertTrue(Files.isDirectory(workspaceRoot),
                    "the workspace root must be created when built-ins are enabled");
        } finally {
            enabledRuntime.close();
        }
    }

    @Test
    void enabledBuiltInsRejectUntrustedOrNonLocalWorkspaceConfiguration() {
        TestComponent component = new TestComponent();
        component.workspaceTools = true;

        AgentConfig missingTrust = config();
        missingTrust.getWorkspace().setRoot("target/untrusted-workspace");
        AgentConfigException trustFailure = assertThrows(
                AgentConfigException.class, () -> component.runtime(missingTrust));
        assertTrue(trustFailure.getMessage().contains("trustedLocal"));

        AgentConfig missingRoot = config();
        missingRoot.getWorkspace().setTrustedLocal(true);
        AgentConfigException rootFailure = assertThrows(
                AgentConfigException.class, () -> component.runtime(missingRoot));
        assertTrue(rootFailure.getMessage().contains("root"));
    }

    @Test
    void shellRegistrationRejectsDisabledModeAndBlankWhitelist(@TempDir Path workspaceRoot) {
        assertInvalidShell(workspaceRoot, config -> config.getShell().setMode(ShellMode.DISABLED));
        assertInvalidShell(workspaceRoot, config -> config.getShell().setWhitelist(null));
        assertInvalidShell(workspaceRoot, config -> config.getShell().setWhitelist(List.of()));
        assertInvalidShell(workspaceRoot, config -> config.getShell().setWhitelist(List.of("java", " ")));
    }

    @Test
    void customizedAgentCannotSilentlyDropLiteFlowToolkitTools() {
        TestComponent component = new TestComponent();
        ContextProbeTool required = new ContextProbeTool();
        component.tools = List.of(required);
        component.agentCustomizer = builder -> {
            ReActAgent snapshot = builder.build();
            try {
                return ReActAgent.builder()
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

        assertTrue(failure.getMessage().contains("Toolkit"));
    }

    private static void assertInvalidShell(
            Path root, java.util.function.Consumer<AgentConfig> invalidator) {
        AgentConfig config = config();
        config.getWorkspace().setRoot(root.toString());
        config.getWorkspace().setTrustedLocal(true);
        config.getShell().setMode(ShellMode.WHITELIST);
        config.getShell().setWhitelist(List.of("java"));
        invalidator.accept(config);
        TestComponent component = new TestComponent();
        component.shellTool = true;
        assertThrows(AgentConfigException.class, () -> component.runtime(config));
    }

    private static AgentConfig config() {
        AgentConfig config = new AgentConfig();
        config.getStateStore().setJsonRoot("target/agent-state");
        config.getRuntime().setNamespace("toolkit-test");
        config.getRuntime().setTimeout(Duration.ofSeconds(2));
        return config;
    }

    private static final class TestComponent extends AgentComponent {
        private final Model model = new ScriptedChatModel("reply");
        private List<Object> tools = List.of();
        private java.util.function.Consumer<Toolkit> toolkitCustomizer = ignored -> { };
        private UnaryOperator<ReActAgent.Builder> agentCustomizer = UnaryOperator.identity();
        private boolean workspaceTools;
        private boolean shellTool;

        private AgentRuntime runtime(AgentConfig config) {
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
        protected boolean enableWorkspaceFileTools() {
            return workspaceTools;
        }

        @Override
        protected boolean enableShellTool() {
            return shellTool;
        }

        @Override
        protected ReActAgent.Builder customizeAgent(ReActAgent.Builder builder) {
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
