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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

    @Test
    void shellCancellationDuringOutputJoinReturnsPromptlyAndTerminatesOrphan() throws Exception {
        LiteFlowAgentContext invocation = AgentTestContexts.liteFlowContext();
        try (OrphanInvocation orphan = OrphanInvocation.start(
                Duration.ofSeconds(5), invocation, "cancel-join")) {
            orphan.awaitReadyAndReleaseParent();

            long cancelledAt = System.nanoTime();
            invocation.cancel();
            String result = orphan.result(Duration.ofSeconds(2));
            long cancellationLatency = System.nanoTime() - cancelledAt;

            assertTrue(result.contains("cancelled"), result);
            assertTrue(cancellationLatency < Duration.ofMillis(500).toNanos(),
                    "cancellation was not observed in bounded slices: "
                            + Duration.ofNanos(cancellationLatency));
            orphan.assertChildExited();
        }
    }

    @Test
    void invocationDeadlineDuringOutputJoinTerminatesOrphan() throws Exception {
        LiteFlowAgentContext invocation = withDeadline(Duration.ofSeconds(2));
        try (OrphanInvocation orphan = OrphanInvocation.start(
                Duration.ofSeconds(5), invocation, "deadline-join")) {
            orphan.awaitReadyAndReleaseParent();
            assertTrue(Instant.now().isBefore(invocation.getDeadline()),
                    "parent must exit before the invocation deadline");

            String result = orphan.result(Duration.ofSeconds(3));

            assertTrue(result.contains("invocation deadline exceeded"), result);
            orphan.assertChildExited();
        }
    }

    @Test
    void shellToolTimeoutDuringOutputJoinTerminatesOrphan() throws Exception {
        Duration toolTimeout = Duration.ofMillis(900);
        LiteFlowAgentContext invocation = AgentTestContexts.liteFlowContext();
        try (OrphanInvocation orphan = OrphanInvocation.start(
                toolTimeout, invocation, "timeout-join")) {
            orphan.awaitReadyAndReleaseParent();

            String result = orphan.result(Duration.ofSeconds(2));
            long elapsed = System.nanoTime() - orphan.startedAtNanos;

            assertEquals("{\"error\":\"timeout after " + toolTimeout + "\"}", result);
            assertTrue(elapsed < toolTimeout.plusMillis(400).toNanos(),
                    "tool timeout was not shared with output join: " + Duration.ofNanos(elapsed));
            orphan.assertChildExited();
        }
    }

    @Test
    void shellCancellationExpandsCapturedFrontierDuringOutputJoin() throws Exception {
        LiteFlowAgentContext invocation = AgentTestContexts.liteFlowContext();
        try (FrontierInvocation frontier = FrontierInvocation.start(invocation, "frontier-cancel")) {
            frontier.awaitParentExitAndOutputJoin();
            frontier.spawnGrandchild();
            new CountDownLatch(1).await(100, TimeUnit.MILLISECONDS);

            invocation.cancel();
            String result = frontier.result(Duration.ofSeconds(2));

            assertTrue(result.contains("cancelled"), result);
            frontier.assertDescendantsExited();
        }
    }

    @Test
    void normalOutputCompletionCleansDescendantsAddedAfterParentExit() throws Exception {
        LiteFlowAgentContext invocation = AgentTestContexts.liteFlowContext();
        try (FrontierInvocation frontier = FrontierInvocation.start(invocation, "frontier-normal")) {
            frontier.awaitParentExitAndOutputJoin();
            frontier.spawnGrandchild();
            frontier.completeOutput();

            String result = frontier.result(Duration.ofSeconds(2));

            assertEquals("", result);
            frontier.assertDescendantsExited();
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

    private static LiteFlowAgentContext withDeadline(Duration duration) {
        LiteFlowAgentContext base = AgentTestContexts.liteFlowContext();
        return new LiteFlowAgentContext(
                base.getIdentity(),
                base.getSlot(),
                base.getChainId(),
                base.getNodeId(),
                base.getRequestId(),
                base.getTraceId(),
                Instant.now().plus(duration),
                base.getOutputSpec(),
                base.getAttachmentKey());
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
            ProcessHandle.of(pid).ifPresent(handle -> {
                handle.destroyForcibly();
                try {
                    handle.onExit().get(2, TimeUnit.SECONDS);
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | TimeoutException ignored) {
                    // Best-effort test teardown after the behavioral assertion has already failed.
                }
            });
        }
    }

    private static long readPidIfPresent(Path path) {
        try {
            return Files.exists(path) ? Long.parseLong(Files.readString(path)) : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static final class OrphanInvocation implements AutoCloseable {
        private final Path parentPid;
        private final Path childPid;
        private final Path release;
        private final ExecutorService executor;
        private final Future<String> call;
        private final long startedAtNanos;

        private OrphanInvocation(
                Path parentPid,
                Path childPid,
                Path release,
                ExecutorService executor,
                Future<String> call,
                long startedAtNanos) {
            this.parentPid = parentPid;
            this.childPid = childPid;
            this.release = release;
            this.executor = executor;
            this.call = call;
            this.startedAtNanos = startedAtNanos;
        }

        private static OrphanInvocation start(
                Duration toolTimeout,
                LiteFlowAgentContext invocation,
                String prefix) throws Exception {
            Path root = Files.createTempDirectory("liteflow-shell-" + prefix + "-");
            AgentConfig shellConfig = shellConfig(root, toolTimeout, 1024);
            GuardedWorkspacePathResolver resolver = new GuardedWorkspacePathResolver(root, 1024);
            ManagedShellCommandTool tool = new ManagedShellCommandTool(resolver, shellConfig);
            Path session = resolver.sessionRoot(invocation.getRuntimeSessionId());
            Path parentPid = session.resolve("parent.pid");
            Path childPid = session.resolve("child.pid");
            Path release = session.resolve("release");
            ExecutorService executor = Executors.newSingleThreadExecutor();
            long startedAtNanos = System.nanoTime();
            Future<String> call = executor.submit(() -> tool.executeCommand(
                    AgentTestContexts.runtimeContext(invocation),
                    fixtureCommand("orphan", "parent.pid", "child.pid", "release")));
            return new OrphanInvocation(
                    parentPid, childPid, release, executor, call, startedAtNanos);
        }

        private void awaitReadyAndReleaseParent() throws Exception {
            awaitFile(childPid, Duration.ofSeconds(2));
            new CountDownLatch(1).await(100, TimeUnit.MILLISECONDS);
            Files.writeString(release, "release");
            assertProcessExited(readPid(parentPid));
        }

        private String result(Duration timeout) throws Exception {
            return call.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        }

        private void assertChildExited() throws Exception {
            assertProcessExited(readPid(childPid));
        }

        @Override
        public void close() {
            call.cancel(true);
            executor.shutdownNow();
            forceKill(readPidIfPresent(childPid));
            forceKill(readPidIfPresent(parentPid));
        }

    }

    private static final class FrontierInvocation implements AutoCloseable {
        private final Path parentPid;
        private final Path childPid;
        private final Path grandchildPid;
        private final Path releaseParent;
        private final Path spawnGrandchild;
        private final Path completeOutput;
        private final ExecutorService executor;
        private final Future<String> call;

        private FrontierInvocation(
                Path parentPid,
                Path childPid,
                Path grandchildPid,
                Path releaseParent,
                Path spawnGrandchild,
                Path completeOutput,
                ExecutorService executor,
                Future<String> call) {
            this.parentPid = parentPid;
            this.childPid = childPid;
            this.grandchildPid = grandchildPid;
            this.releaseParent = releaseParent;
            this.spawnGrandchild = spawnGrandchild;
            this.completeOutput = completeOutput;
            this.executor = executor;
            this.call = call;
        }

        private static FrontierInvocation start(
                LiteFlowAgentContext invocation, String prefix) throws Exception {
            Path root = Files.createTempDirectory("liteflow-shell-" + prefix + "-");
            AgentConfig shellConfig = shellConfig(root, Duration.ofSeconds(5), 1024);
            GuardedWorkspacePathResolver resolver = new GuardedWorkspacePathResolver(root, 1024);
            ManagedShellCommandTool tool = new ManagedShellCommandTool(resolver, shellConfig);
            Path session = resolver.sessionRoot(invocation.getRuntimeSessionId());
            Path parentPid = session.resolve("parent.pid");
            Path childPid = session.resolve("child.pid");
            Path grandchildPid = session.resolve("grandchild.pid");
            Path releaseParent = session.resolve("release-parent");
            Path spawnGrandchild = session.resolve("spawn-grandchild");
            Path completeOutput = session.resolve("complete-output");
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> call = executor.submit(() -> tool.executeCommand(
                    AgentTestContexts.runtimeContext(invocation),
                    fixtureCommand(
                            "frontier",
                            "parent.pid",
                            "child.pid",
                            "release-parent",
                            "spawn-grandchild",
                            "grandchild.pid",
                            "complete-output")));
            return new FrontierInvocation(
                    parentPid,
                    childPid,
                    grandchildPid,
                    releaseParent,
                    spawnGrandchild,
                    completeOutput,
                    executor,
                    call);
        }

        private void awaitParentExitAndOutputJoin() throws Exception {
            awaitFile(childPid, Duration.ofSeconds(2));
            new CountDownLatch(1).await(100, TimeUnit.MILLISECONDS);
            Files.writeString(releaseParent, "release");
            assertProcessExited(readPid(parentPid));
        }

        private void spawnGrandchild() throws Exception {
            Files.writeString(spawnGrandchild, "spawn");
            awaitFile(grandchildPid, Duration.ofSeconds(2));
        }

        private void completeOutput() throws Exception {
            Files.writeString(completeOutput, "complete");
        }

        private String result(Duration timeout) throws Exception {
            return call.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        }

        private void assertDescendantsExited() throws Exception {
            assertProcessExited(readPid(childPid));
            assertProcessExited(readPid(grandchildPid));
        }

        @Override
        public void close() {
            call.cancel(true);
            executor.shutdownNow();
            forceKill(readPidIfPresent(grandchildPid));
            forceKill(readPidIfPresent(childPid));
            forceKill(readPidIfPresent(parentPid));
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
