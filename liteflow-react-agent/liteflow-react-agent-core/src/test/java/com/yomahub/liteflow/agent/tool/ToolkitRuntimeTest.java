package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.ReActAgentRuntime;
import com.yomahub.liteflow.agent.testsupport.AgentTestContexts;
import com.yomahub.liteflow.agent.testsupport.ScriptedChatModel;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

        ReActAgentRuntime runtime = component.runtime(config());
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
    void guardedLocalBuiltInsAreDisabledByDefaultAndRequireAWorkspaceLeaseWhenEnabled() {
        TestComponent disabled = new TestComponent();
        ReActAgentRuntime disabledRuntime = disabled.runtime(config());
        try {
            assertFalse(disabledRuntime.agent().getToolkit().getToolNames().contains("read_file"));
            assertFalse(disabledRuntime.agent().getToolkit().getToolNames()
                    .contains("execute_shell_command"));
            assertFalse(disabled.workspaceLeaseRequired());
        } finally {
            disabledRuntime.close();
        }

        AgentConfig enabledConfig = config();
        enabledConfig.getWorkspace().setRoot(Path.of("target", "toolkit-workspace").toString());
        enabledConfig.getWorkspace().setTrustedLocal(true);
        enabledConfig.getShell().setMode(ShellMode.WHITELIST);
        TestComponent enabled = new TestComponent();
        enabled.workspaceTools = true;
        enabled.shellTool = true;
        ReActAgentRuntime enabledRuntime = enabled.runtime(enabledConfig);
        try {
            assertTrue(enabledRuntime.agent().getToolkit().getToolNames().contains("read_file"));
            assertTrue(enabledRuntime.agent().getToolkit().getToolNames()
                    .contains("execute_shell_command"));
            assertTrue(enabled.workspaceLeaseRequired());
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

    @Test
    void shellDrainsExcessOutputAndReturnsAnExplicitTruncationMarker() throws Exception {
        Path root = Files.createTempDirectory("liteflow-shell-drain-");
        AgentConfig shellConfig = shellConfig(root, Duration.ofMillis(500), 64);
        ManagedShellCommandTool tool = new ManagedShellCommandTool(root, shellConfig);

        String result = tool.executeCommand(
                AgentTestContexts.runtimeContext(AgentTestContexts.liteFlowContext()),
                fixtureCommand("flood", "2097152"));

        assertTrue(result.contains("truncated after 64 bytes"), result);
        assertFalse(result.contains("timeout"), result);
    }

    @Test
    void shellRegistrationRejectsInvalidTimeoutOutputAndModeLists() throws Exception {
        Path root = Files.createTempDirectory("liteflow-shell-config-");
        assertInvalidShell(root, config -> config.getShell().setTimeout(Duration.ZERO));
        assertInvalidShell(root, config -> config.getShell()
                .setTimeout(Duration.ofSeconds(Long.MAX_VALUE)));
        assertInvalidShell(root, config -> config.getShell().setMaxOutputBytes(0));
        assertInvalidShell(root, config -> config.getShell().setMaxOutputBytes(Long.MAX_VALUE));
        assertInvalidShell(root, config -> config.getShell().setWhitelist(null));
        assertInvalidShell(root, config -> config.getShell().setWhitelist(List.of("java", " ")));
        assertInvalidShell(root, config -> {
            config.getShell().setMode(ShellMode.BLACKLIST);
            config.getShell().setBlacklist(null);
        });
        assertInvalidShell(root, config -> {
            config.getShell().setMode(ShellMode.BLACKLIST);
            config.getShell().setBlacklist(List.of("rm", ""));
        });
    }

    @Test
    void shellTimeoutTerminatesParentAndDescendantProcesses() throws Exception {
        Path root = Files.createTempDirectory("liteflow-shell-timeout-");
        AgentConfig shellConfig = shellConfig(root, Duration.ofMillis(750), 1024);
        GuardedWorkspacePathResolver resolver = new GuardedWorkspacePathResolver(root, 1024);
        ManagedShellCommandTool tool = new ManagedShellCommandTool(resolver, shellConfig);
        LiteFlowAgentContext invocation = AgentTestContexts.liteFlowContext();
        Path session = resolver.sessionRoot(invocation.getRuntimeSessionId());
        Path parentPid = session.resolve("parent.pid");
        Path childPid = session.resolve("child.pid");
        long parent = -1;
        long child = -1;
        try {
            String result = tool.executeCommand(
                    AgentTestContexts.runtimeContext(invocation),
                    fixtureCommand("tree", "parent.pid", "child.pid"));
            assertTrue(result.contains("timeout"), result);
            parent = readPid(parentPid);
            child = readPid(childPid);
            assertProcessExited(parent);
            assertProcessExited(child);
        } finally {
            forceKill(child);
            forceKill(parent);
        }
    }

    @Test
    void shellCancellationTerminatesParentAndDescendantProcesses() throws Exception {
        Path root = Files.createTempDirectory("liteflow-shell-cancel-");
        AgentConfig shellConfig = shellConfig(root, Duration.ofSeconds(5), 1024);
        GuardedWorkspacePathResolver resolver = new GuardedWorkspacePathResolver(root, 1024);
        ManagedShellCommandTool tool = new ManagedShellCommandTool(resolver, shellConfig);
        LiteFlowAgentContext invocation = AgentTestContexts.liteFlowContext();
        Path session = resolver.sessionRoot(invocation.getRuntimeSessionId());
        Path parentPid = session.resolve("parent.pid");
        Path childPid = session.resolve("child.pid");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> call = executor.submit(() -> tool.executeCommand(
                AgentTestContexts.runtimeContext(invocation),
                fixtureCommand("tree", "parent.pid", "child.pid")));
        long parent = -1;
        long child = -1;
        try {
            awaitFile(childPid, Duration.ofSeconds(2));
            parent = readPid(parentPid);
            child = readPid(childPid);
            invocation.cancel();
            String result = call.get(2, TimeUnit.SECONDS);
            assertTrue(result.contains("cancelled"), result);
            assertProcessExited(parent);
            assertProcessExited(child);
        } finally {
            call.cancel(true);
            executor.shutdownNow();
            forceKill(child);
            forceKill(parent);
        }
    }

    private static AgentConfig shellConfig(Path root, Duration timeout, long maxOutputBytes) {
        AgentConfig config = config();
        config.getWorkspace().setRoot(root.toString());
        config.getWorkspace().setTrustedLocal(true);
        config.getShell().setMode(ShellMode.WHITELIST);
        config.getShell().setWhitelist(List.of(javaExecutable()));
        config.getShell().setTimeout(timeout);
        config.getShell().setMaxOutputBytes(maxOutputBytes);
        return config;
    }

    private static void assertInvalidShell(
            Path root, java.util.function.Consumer<AgentConfig> invalidator) {
        AgentConfig config = shellConfig(root, Duration.ofSeconds(1), 1024);
        invalidator.accept(config);
        TestComponent component = new TestComponent();
        component.shellTool = true;
        assertThrows(AgentConfigException.class, () -> component.runtime(config));
    }

    private static String fixtureCommand(String... arguments) {
        String testClasses = Path.of("target", "test-classes").toAbsolutePath().toString();
        return String.join(" ", java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(
                                javaExecutable(), "-cp", testClasses,
                                ShellProcessFixture.class.getName()),
                        java.util.Arrays.stream(arguments))
                .toList());
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private static void awaitFile(Path path, Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (!Files.exists(path) && Instant.now().isBefore(deadline)) {
            Thread.onSpinWait();
        }
        assertTrue(Files.exists(path), "fixture did not publish pid file: " + path);
    }

    private static long readPid(Path path) throws Exception {
        awaitFile(path, Duration.ofSeconds(1));
        return Long.parseLong(Files.readString(path));
    }

    private static void assertProcessExited(long pid) throws Exception {
        ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
        if (handle != null && handle.isAlive()) {
            handle.onExit().get(2, TimeUnit.SECONDS);
        }
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false),
                "process is still alive: " + pid);
    }

    private static void forceKill(long pid) {
        if (pid > 0) {
            ProcessHandle.of(pid).ifPresent(ProcessHandle::destroyForcibly);
        }
    }

    private static AgentConfig config() {
        AgentConfig config = new AgentConfig();
        config.getRuntime().setNamespace("toolkit-test");
        config.getRuntime().setTimeout(Duration.ofSeconds(2));
        return config;
    }

    private static final class TestComponent extends ReActAgentComponent {
        private final Model model = new ScriptedChatModel("reply");
        private List<Object> tools = List.of();
        private java.util.function.Consumer<Toolkit> toolkitCustomizer = ignored -> { };
        private UnaryOperator<ReActAgent.Builder> agentCustomizer = UnaryOperator.identity();
        private boolean workspaceTools;
        private boolean shellTool;

        private ReActAgentRuntime runtime(AgentConfig config) {
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
