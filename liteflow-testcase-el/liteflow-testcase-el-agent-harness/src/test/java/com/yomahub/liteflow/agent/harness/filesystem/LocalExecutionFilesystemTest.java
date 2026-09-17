package com.yomahub.liteflow.agent.harness.filesystem;

import com.yomahub.liteflow.agent.harness.storage.StoredWorkspaceFilesystem;
import com.yomahub.liteflow.property.agent.HarnessConfig;
import com.yomahub.liteflow.property.agent.ShellConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.remote.store.InMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs({OS.MAC, OS.LINUX})
class LocalExecutionFilesystemTest {
    @TempDir Path temp;
    private final RuntimeContext first = RuntimeContext.builder().userId("user").sessionId("first").build();
    private final RuntimeContext second = RuntimeContext.builder().userId("user").sessionId("second").build();

    private AbstractFilesystem files(boolean remote) {
        return remote ? new StoredWorkspaceFilesystem(new InMemoryStore(),
                rc -> List.of("workspace", rc.getUserId(), rc.getSessionId()), temp)
                : new GuardedLocalFilesystem(temp.resolve("files"));
    }

    private LocalExecutionFilesystem shell(AbstractFilesystem files) {
        return new LocalExecutionFilesystem(files, temp, policy(Duration.ofSeconds(10)), "app");
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void shellSharesFilesWithToolsAndPersistsChangesAcrossRecreation(boolean remote) {
        AbstractFilesystem files = files(remote);
        assertTrue(files.write(first, "input.txt", "hello").isSuccess());
        assertTrue(files.write(first, "removed.txt", "old").isSuccess());
        var result = shell(files).execute(first, script("cat input.txt > output.txt; printf ' world' >> output.txt; rm removed.txt; printf '\\000\\377' > binary.bin; printf shell-ok"), null);
        assertEquals(0, result.exitCode(), result.output());
        assertEquals("shell-ok", result.output());
        assertEquals("hello world", files.read(first, "output.txt", 0, 0).fileData().content());
        assertArrayEquals(new byte[]{0, (byte) 255}, files.downloadFiles(first, List.of("binary.bin")).get(0).content());
        assertFalse(files.exists(first, "removed.txt"));
        assertFalse(files.exists(second, "output.txt"));
        assertTrue(shell(files).execute(first, script("cat output.txt"), null).output().contains("hello world"));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void independentSessionsCanExecuteConcurrentlyWithoutSharingFiles(boolean remote) throws Exception {
        AbstractFilesystem files = files(remote);
        files.write(first, "input.txt", "first");
        files.write(second, "input.txt", "second");
        LocalExecutionFilesystem shell = shell(files);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = executor.submit(() -> assertEquals(0, shell.execute(first, script("sleep 0.1; cp input.txt output.txt"), null).exitCode()));
            Future<?> b = executor.submit(() -> assertEquals(0, shell.execute(second, script("cp input.txt output.txt"), null).exitCode()));
            a.get(5, TimeUnit.SECONDS);
            b.get(5, TimeUnit.SECONDS);
            assertEquals("first", files.read(first, "output.txt", 0, 0).fileData().content());
            assertEquals("second", files.read(second, "output.txt", 0, 0).fileData().content());
        } finally { executor.shutdownNow(); }
    }

    @Test void commandFailureStillPersistsFilesAndReturnsExitCode() {
        AbstractFilesystem files = files(true);
        var result = shell(files).execute(first, script("printf partial > partial.txt; printf problem >&2; exit 7"), null);
        assertEquals(7, result.exitCode());
        assertTrue(result.output().contains("problem"));
        assertEquals("partial", files.read(first, "partial.txt", 0, 0).fileData().content());
    }

    @Test void serverTimeoutCapsRequestedTimeoutAndTerminatesChild() {
        AbstractFilesystem files = files(true);
        LocalExecutionFilesystem shell = new LocalExecutionFilesystem(files, temp, policy(Duration.ofMillis(300)), "app");
        long start = System.nanoTime();
        var result = shell.execute(first, script("sleep 30 & echo $! > child.pid; wait"), 300);
        assertEquals(124, result.exitCode(), result.output());
        assertTrue(Duration.ofNanos(System.nanoTime() - start).toSeconds() < 5);
        long pid = Long.parseLong(files.read(first, "child.pid", 0, 0).fileData().content().strip());
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
    }

    @Test void interruptionStopsTheCommandBeforeReturning() throws Exception {
        Path pidFile = temp.resolve("interrupted.pid");
        var shell = shell(files(true));
        Thread worker = new Thread(() -> shell.execute(first,
                script("echo $$ > '" + pidFile + "'; exec sleep 30"), null));
        worker.start();
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!Files.exists(pidFile) && System.nanoTime() < deadline) Thread.sleep(10);
            assertTrue(Files.exists(pidFile));
            long pid = Long.parseLong(Files.readString(pidFile).strip());
            worker.interrupt();
            worker.join(4000);
            assertFalse(worker.isAlive());
            assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
        } finally { worker.interrupt(); worker.join(4000); }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void synchronizesLargeCommandOutputsAndRematerializesThem(boolean remote) {
        AbstractFilesystem files = files(remote);
        files.write(first, "input.txt", "keep");
        var shell = shell(files);
        var result = shell.execute(first, script("rm input.txt; head -c 11534336 /dev/zero > huge.bin"), null);
        assertEquals(0, result.exitCode(), result.output());
        assertFalse(files.exists(first, "input.txt"));
        assertEquals(11534336, files.downloadFiles(first, List.of("huge.bin")).get(0).content().length);
        var next = shell(files).execute(first, script("wc -c huge.bin"), null);
        assertEquals(0, next.exitCode(), next.output());
        assertTrue(next.output().contains("11534336"));
    }

    @Test void rejectsSymlinkOutputWithoutFollowingOrDeletingItsTarget() throws Exception {
        Path outside = temp.resolve("outside.txt");
        Files.writeString(outside, "keep");
        var result = shell(files(true)).execute(first, script("ln -s '" + outside + "' link.txt"), null);
        assertNotEquals(0, result.exitCode());
        assertEquals("keep", Files.readString(outside));
    }

    @Test void outputCaptureIsBoundedAndDoesNotBlockOnFullPipes() {
        var result = shell(files(true)).execute(first, script("yes line | head -c 200000"), null);
        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.truncated());
        assertEquals(100_000, result.output().getBytes(StandardCharsets.UTF_8).length);
    }

    private static ShellConfig policy(Duration timeout) {
        ShellConfig shell = new ShellConfig();
        shell.setMode(ShellMode.WHITELIST);
        shell.setWhitelist(List.of("sh", "pwd", "cat", "node", "python3"));
        shell.setTimeout(timeout);
        return shell;
    }

    private static String script(String command) {
        return "sh -c '" + command.replace("'", "'\\''") + "'";
    }

    @Test void requiresTheExistingShellPolicyWithoutAnAdditionalTrustFlag() {
        HarnessConfig config = new HarnessConfig();
        assertDoesNotThrow(config::validate);
        ShellConfig shell = new ShellConfig();
        assertDoesNotThrow(() -> new LocalExecutionFilesystem(files(true), temp, shell, "app"));
        shell.setMode(ShellMode.DISABLED);
        assertThrows(IllegalArgumentException.class,
                () -> new LocalExecutionFilesystem(files(true), temp, shell, "app"));
        shell.setMode(ShellMode.WHITELIST);
        shell.setWhitelist(List.of());
        assertThrows(IllegalArgumentException.class,
                () -> new LocalExecutionFilesystem(files(true), temp, shell, "app"));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void configuredDirectoryIsStableAndKeepsEmptyDirectoriesAndExecutableBits(boolean remote) throws Exception {
        AbstractFilesystem files = files(remote);
        LocalExecutionFilesystem shell = shell(files);
        Path cwd = shell.executionDirectory(first);
        assertTrue(cwd.startsWith(temp.toRealPath()));
        assertEquals(cwd, Path.of(shell.execute(first, "pwd", null).output().strip()));
        assertEquals(0, shell.execute(first, script("mkdir empty; printf '#!/bin/sh\\necho persisted' > run.sh; chmod +x run.sh"), null).exitCode());
        LocalExecutionFilesystem reopened = shell(files);
        assertEquals(cwd, reopened.executionDirectory(first));
        var next = reopened.execute(first, "./run.sh", null);
        assertEquals(0, next.exitCode(), next.output());
        assertTrue(next.output().contains("persisted"));
        assertTrue(Files.isDirectory(cwd.resolve("empty")));
    }

    @Test void directoryUsesPlainApplicationAndConversationOnly() throws Exception {
        AbstractFilesystem files = files(true);
        var a = shell(files);
        var otherUser = RuntimeContext.builder().userId("other").sessionId("first").build();
        var otherApp = new LocalExecutionFilesystem(files, temp, policy(Duration.ofSeconds(5)), "other-app");
        assertNotEquals(a.executionDirectory(first), a.executionDirectory(second));
        assertEquals(a.executionDirectory(first), a.executionDirectory(otherUser));
        assertEquals(temp.toRealPath().resolve("app/first"), a.executionDirectory(first));
        assertNotEquals(a.executionDirectory(first), otherApp.executionDirectory(first));
    }

    @Test void refreshesAuthoritativeChangesAndDeletionsBeforeTheNextCommand() throws Exception {
        AbstractFilesystem files = files(true);
        files.write(first, "input.txt", "old");
        LocalExecutionFilesystem shell = shell(files);
        assertEquals("old", shell.execute(first, "cat input.txt", null).output());
        files.uploadFiles(first, List.of(Map.entry("input.txt", "new".getBytes(StandardCharsets.UTF_8))));
        assertEquals("new", shell.execute(first, "cat input.txt", null).output());
        files.delete(first, "input.txt");
        shell.execute(first, "pwd", null);
        assertFalse(Files.exists(shell.executionDirectory(first).resolve("input.txt")));
    }

    @Test void refreshHandlesAnAuthoritativeFileBecomingADirectoryAndBack() {
        AbstractFilesystem files = files(true);
        files.write(first, "changed", "file");
        LocalExecutionFilesystem shell = shell(files);
        assertEquals(0, shell.execute(first, "pwd", null).exitCode());
        files.delete(first, "changed");
        files.write(first, "changed/child.txt", "nested");
        assertEquals("nested", shell.execute(first, "cat changed/child.txt", null).output());
        files.delete(first, "changed");
        files.write(first, "changed", "file again");
        assertEquals("file again", shell.execute(first, "cat changed", null).output());
    }

    @Test void failedSynchronizationRetainsFilesInsteadOfSilentlyOverwritingThem() throws Exception {
        InMemoryStore unavailableStore = new InMemoryStore() {
            @Override
            public void put(List<String> namespace, String key, Map<String, Object> value) {
                throw new IllegalStateException("storage unavailable");
            }
        };
        AbstractFilesystem files = new StoredWorkspaceFilesystem(unavailableStore,
                rc -> List.of(rc.getSessionId()), temp);
        LocalExecutionFilesystem shell = new LocalExecutionFilesystem(files, temp, policy(Duration.ofSeconds(5)), "app");
        assertNotEquals(0, shell.execute(first, script("printf unsaved > result.txt"), null).exitCode());
        Path output = shell.executionDirectory(first).resolve("result.txt");
        assertEquals("unsaved", Files.readString(output));
        var retry = shell(files).execute(first, "pwd", null);
        assertNotEquals(0, retry.exitCode());
        assertTrue(retry.output().contains("Unsynchronized files"));
        var independent = RuntimeContext.builder().sessionId("first.pending").build();
        assertEquals(0, shell.execute(independent, "pwd", null).exitCode());
        assertEquals("unsaved", Files.readString(output));
    }

    @Test void validatesCommandsWithoutTreatingWorkingDirectoryAsAnotherShellCommand() throws Exception {
        LocalExecutionFilesystem shell = shell(files(true));
        assertNotEquals(0, shell.execute(first, "uname", null).exitCode());
        assertNotEquals(0, shell.execute(first, "pwd && pwd", null).exitCode());
        assertEquals(0, shell.execute(first, script("mkdir nested"), null).exitCode());
        String result = shell.executeCommand(first, "pwd", "nested", null);
        assertTrue(result.startsWith("Exit code: 0"), result);
        assertTrue(result.contains(shell.executionDirectory(first).resolve("nested").toString()), result);
        assertFalse(shell.executeCommand(first, "pwd", "../", null).startsWith("Exit code: 0"));
    }
}
