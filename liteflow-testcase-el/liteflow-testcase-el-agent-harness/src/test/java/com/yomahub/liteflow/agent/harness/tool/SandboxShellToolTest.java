package com.yomahub.liteflow.agent.harness.tool;

import com.yomahub.liteflow.property.agent.ShellConfig;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.filesystem.model.ExecuteResponse;
import io.agentscope.harness.agent.filesystem.sandbox.AbstractSandboxFilesystem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SandboxShellToolTest {
    private record Call(RuntimeContext context, String command, Integer timeout) { }

    private AbstractSandboxFilesystem sandbox(List<Call> calls) {
        return (AbstractSandboxFilesystem) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{AbstractSandboxFilesystem.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("execute")) {
                        calls.add(new Call((RuntimeContext) arguments[0], (String) arguments[1], (Integer) arguments[2]));
                        return new ExecuteResponse("from-container", 0, false);
                    }
                    throw new AssertionError("Unexpected host/filesystem operation: " + method.getName());
                });
    }

    @Test void defaultsAllowShellPythonAndNodeAndDelegateOnlyToTheSelectedSandbox() {
        List<Call> calls = new ArrayList<>();
        SandboxShellTool tool = new SandboxShellTool(sandbox(calls), new ShellConfig(), true);
        RuntimeContext context = RuntimeContext.builder().userId("user").sessionId("session").build();
        for (String command : List.of("sh -c 'printf hello'", "python3 --version", "node --version")) {
            assertTrue(tool.execute(context, command, null, null).contains("from-container"));
        }
        assertEquals(3, calls.size());
        assertSame(context, calls.get(0).context());
        assertEquals("node --version", calls.get(2).command());
        assertEquals(60, calls.get(2).timeout());
    }

    @Test void customListReplacesDefaultsAndRejectedCommandsNeverReachTheContainer() {
        List<Call> calls = new ArrayList<>();
        ShellConfig config = new ShellConfig();
        config.setWhitelist(List.of("printf"));
        SandboxShellTool tool = new SandboxShellTool(sandbox(calls), config, true);
        assertTrue(tool.execute(null, "python3 --version", null, null).contains("Command rejected"));
        assertTrue(tool.execute(null, "printf a && printf b", null, null).contains("Command rejected"));
        assertEquals(0, calls.size());
        assertTrue(tool.execute(null, "printf hello", null, null).startsWith("Exit code: 0"));
        assertEquals(1, calls.size());
    }

    @Test void workingDirectoryIsQuotedSeparatelyAndTimeoutCannotExceedTheServerLimit() {
        List<Call> calls = new ArrayList<>();
        ShellConfig config = new ShellConfig();
        config.setTimeout(Duration.ofSeconds(3));
        SandboxShellTool tool = new SandboxShellTool(sandbox(calls), config, true);
        assertTrue(tool.execute(null, "pwd", "nested dir", 300).startsWith("Exit code: 0"));
        assertEquals("cd 'nested dir' && pwd", calls.get(0).command());
        assertEquals(3, calls.get(0).timeout());
        for (String invalid : List.of("../other", "/tmp", "C:\\temp", "~")) {
            assertTrue(tool.execute(null, "pwd", invalid, 1).contains("must be relative"));
        }
        assertEquals(1, calls.size());
    }

    @Test void explicitlyEmptyCommandListsAreInvalid() {
        ShellConfig config = new ShellConfig();
        config.setWhitelist(List.of());
        assertThrows(IllegalArgumentException.class, () -> new SandboxShellTool(sandbox(new ArrayList<>()), config, true));
    }
}
