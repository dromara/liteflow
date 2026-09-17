package com.yomahub.liteflow.agent.harness.storage;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.filesystem.remote.store.InMemoryStore;
import io.agentscope.harness.agent.workspace.WorkspaceManager;
import com.yomahub.liteflow.property.agent.AgentConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ManagedStorageTest {
    @TempDir Path temp;
    private RuntimeContext context(String user, String session) {
        return RuntimeContext.builder().sessionId(session).build();
    }

    @Test void backgroundTranscriptMirrorsPreserveTheDatabaseRouteWithAnActiveSandbox() {
        var store = new InMemoryStore();
        var rc = context("alice", "one");
        var filesystem = new ManagedSandboxFilesystem(store, "app", "agent", temp, "/workspace");
        var sandboxCalls = new java.util.concurrent.atomic.AtomicInteger();
        var sandbox = (io.agentscope.harness.agent.sandbox.Sandbox) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{io.agentscope.harness.agent.sandbox.Sandbox.class},
                (proxy, method, args) -> {
                    sandboxCalls.incrementAndGet();
                    throw new IllegalStateException("Transcript must never enter the business sandbox");
                });
        filesystem.setSandbox(sandbox);
        String relativePath = "agents/a/sessions/one.context.jsonl";
        var tree = new io.agentscope.harness.agent.memory.session.SessionTree(
                temp.resolve(relativePath), temp, filesystem).setRuntimeContext(rc);
        tree.append(new io.agentscope.harness.agent.memory.session.SessionEntry.MessageEntry(
                null, "user", "database-only-marker"));
        tree.flush();
        assertTrue(io.agentscope.harness.agent.memory.session.SessionTree.awaitMirrorQuiescence(
                5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0, sandboxCalls.get());
        assertTrue(filesystem.read(rc, relativePath, 0, 0).fileData().content().contains("database-only-marker"));
    }
    @Test void memoryArchivesTasksAndPlansUseOneStoreWithoutAnActiveContainer() throws Exception {
        var store = new InMemoryStore();
        var rc = context("alice", "one");
        var filesystem = new ManagedSandboxFilesystem(store, "app", "agent", temp, "/workspace");
        var workspace = new WorkspaceManager(temp, filesystem);
        for (String path : List.of("memory/2026-09-08.md", "MEMORY.md", "agents/a/sessions/one.log.jsonl",
                "agents/a/tasks/one.json", ".agentscope/bus/item.json", "plans/one.md")) {
            workspace.appendUtf8WorkspaceRelative(rc, path, "first");
            workspace.appendUtf8WorkspaceRelative(rc, path, "second");
            assertEquals("firstsecond", filesystem.read(rc, "/workspace/" + path, 0, 0).fileData().content());
            assertEquals("firstsecond", filesystem.read(rc, temp.resolve("one").resolve(path).toString(), 0, 0).fileData().content());
            assertFalse(Files.exists(temp.resolve(path)));
        }
        var reopened = new ManagedSandboxFilesystem(store, "app", "other-agent", temp.resolve("another-host"), "/workspace");
        assertEquals("firstsecond", reopened.read(rc, "MEMORY.md", 0, 0).fileData().content());
        assertFalse(reopened.exists(context("alice", "two"), "MEMORY.md"));
        assertTrue(reopened.exists(context("bob", "one"), "MEMORY.md"));
        assertFalse(new ManagedSandboxFilesystem(store, "other-app", "agent", temp, "/workspace").exists(rc, "MEMORY.md"));
        assertThrows(IllegalArgumentException.class, () -> filesystem.write(rc, "memory/../private.txt", "unsafe"));
        assertFalse(filesystem.move(rc, "MEMORY.md", "output.txt").isSuccess());
    }
    @Test void failedSnapshotUploadKeepsThePreviousCompleteGeneration() throws Exception {
        var store = new InMemoryStore();
        var snapshots = new StoreSnapshotClient(store, "app");
        byte[] expected = new byte[1_500_000]; new Random(9).nextBytes(expected);
        snapshots.upload("snapshot", new ByteArrayInputStream(expected));
        assertArrayEquals(expected, snapshots.download("snapshot").readAllBytes());
        InputStream broken = new InputStream() {
            int remaining = 600_000;
            @Override public int read() throws IOException { if (--remaining < 0) throw new IOException("broken archive"); return 12; }
        };
        assertThrows(IOException.class, () -> snapshots.upload("snapshot", broken));
        assertArrayEquals(expected, new StoreSnapshotClient(store, "app").download("snapshot").readAllBytes());
        assertThrows(FileNotFoundException.class, () -> snapshots.download("missing"));
        snapshots.upload("snapshot", new ByteArrayInputStream("new".getBytes()));
        assertEquals("new", new String(snapshots.download("snapshot").readAllBytes()));
    }
    @Test void legacyArchiveImportsRecordsAndKeepsOnlyBusinessFilesInTheRemoteTar() throws Exception {
        var archiveBytes = new ByteArrayOutputStream();
        try (var tar = new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(archiveBytes)) {
            for (String path : List.of("./MEMORY.md", "./agents/a/sessions/one.log.jsonl", "./result.txt")) {
                byte[] content = path.getBytes();
                var entry = new org.apache.commons.compress.archivers.tar.TarArchiveEntry(path); entry.setSize(content.length);
                tar.putArchiveEntry(entry); tar.write(content); tar.closeArchiveEntry();
            }
        }
        var local = new io.agentscope.harness.agent.sandbox.snapshot.LocalSnapshotSpec(temp).build("old");
        local.persist(new ByteArrayInputStream(archiveBytes.toByteArray()));
        var store = new InMemoryStore();
        var remote = new io.agentscope.harness.agent.sandbox.snapshot.RemoteSnapshotSpec(new StoreSnapshotClient(store, "app")).build("old");
        var fs = new ManagedSandboxFilesystem(store, "app", "agent", temp, "/workspace");
        var rc = context("alice", "one");
        LegacySnapshotMigration.migrate(local, remote, fs, rc);
        assertEquals("./MEMORY.md", fs.read(rc, "MEMORY.md", 0, 0).fileData().content());
        assertTrue(fs.exists(rc, "agents/a/sessions/one.log.jsonl"));
        try (var tar = new org.apache.commons.compress.archivers.tar.TarArchiveInputStream(remote.restore())) {
            assertEquals("./result.txt", tar.getNextEntry().getName());
            assertNull(tar.getNextEntry());
        }
        assertArrayEquals(archiveBytes.toByteArray(), Files.readAllBytes(temp.resolve("old.tar")));
        fs.edit(rc, "MEMORY.md", "./MEMORY.md", "newer memory", false);
        LegacySnapshotMigration.migrate(local, remote, fs, rc);
        assertEquals("newer memory", fs.read(rc, "MEMORY.md", 0, 0).fileData().content());
    }
    @Test void stagingIgnoresLocalExecutionRootAndRejectsManagedProjectionRoots() throws Exception {
        AgentConfig config = new AgentConfig();
        config.getHarness().setFilesystemBackend(com.yomahub.liteflow.property.agent.HarnessFilesystemBackend.DOCKER);
        config.getHarness().getLocal().setWorkspaceRoot(temp.toString());
        config.getSessionStore().setJsonWorkspaceRoot(temp.toString());
        Files.createDirectories(temp.resolve("skills/example"));
        Files.writeString(temp.resolve("skills/example/SKILL.md"), "static skill");
        Files.createDirectories(temp.resolve("memory"));
        Files.writeString(temp.resolve("memory/old.md"), "old record");
        Path staged;
        try (var staging = StaticWorkspaceStaging.create(config)) {
            staged = staging.root();
            assertFalse(Files.exists(staged.resolve("skills/example/SKILL.md")));
            assertFalse(Files.exists(staged.resolve("memory")));
        }
        assertFalse(Files.exists(staged));
        assertTrue(Files.exists(temp.resolve("memory/old.md")));
        config.getHarness().getDocker().setWorkspaceProjectionRoots(List.of("memory"));
        assertThrows(RuntimeException.class, () -> StaticWorkspaceStaging.create(config));
    }
    @Test void localStagingDoesNotReadDockerSettings() throws Exception {
        AgentConfig config = new AgentConfig();
        config.getHarness().setDocker(null);
        try (var staging = StaticWorkspaceStaging.create(config)) {
            assertTrue(Files.isDirectory(staging.root()));
        }
    }
}
