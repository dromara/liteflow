package com.yomahub.liteflow.agent.harness.filesystem;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.model.Model;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.bus.AsyncToolRecord;
import io.agentscope.harness.agent.bus.AsyncToolRegistry;
import io.agentscope.harness.agent.bus.MessageBus;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.OverlayFilesystem;
import io.agentscope.harness.agent.filesystem.local.LocalFilesystemWithShell;
import io.agentscope.harness.agent.filesystem.model.EditResult;
import io.agentscope.harness.agent.filesystem.model.FileDownloadResponse;
import io.agentscope.harness.agent.filesystem.model.FileUploadResponse;
import io.agentscope.harness.agent.filesystem.model.GlobResult;
import io.agentscope.harness.agent.filesystem.model.LsResult;
import io.agentscope.harness.agent.filesystem.model.ReadResult;
import io.agentscope.harness.agent.filesystem.model.WriteResult;
import io.agentscope.harness.agent.middleware.SubagentEntry;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.tool.MemorySearchTool;
import io.agentscope.harness.agent.tool.ShellExecuteTool;
import io.agentscope.harness.agent.workspace.WorkspaceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardedLocalFilesystemTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsCrossPlatformAbsoluteTraversalNulBlankAndWin32AliasesBeforeCreatingSessionRoot()
            throws Exception {
        Path root = tempDir.resolve("workspace");
        GuardedLocalFilesystem filesystem = new GuardedLocalFilesystem(root, 1024);
        RuntimeContext context = context("session-a", "user-a");
        List<String> rejected = List.of(
                "/etc/passwd",
                "C:\\Windows\\system.ini",
                "C:/Windows/system.ini",
                "C:Windows\\system.ini",
                "\\\\server\\share\\secret",
                "//server/share/secret",
                "\\rooted\\secret",
                "../secret",
                "safe/../secret",
                "safe\\..\\secret",
                "safe\\../secret",
                ".. ",
                "...",
                ". ",
                "safe/.. /secret",
                "safe\\.../secret",
                "safe/   /secret",
                "",
                "   ",
                "safe\0secret");

        for (String path : rejected) {
            assertThrows(SecurityException.class, () -> filesystem.ls(context, path), path);
        }
        assertThrows(SecurityException.class, () -> filesystem.ls(context, null));

        try (Stream<Path> children = Files.list(root)) {
            assertEquals(0, children.count());
        }

        Path outside = tempDir.resolve("outside.txt").toAbsolutePath();
        assertThrows(
                SecurityException.class,
                () -> filesystem.write(context, outside.toString(), "denied"));
        assertFalse(Files.exists(outside));
    }

    @Test
    void preservesTask2SafeWin32TrailingNamesWithoutRewriting() {
        GuardedLocalFilesystem filesystem =
                new GuardedLocalFilesystem(tempDir.resolve("workspace"), 1024);
        RuntimeContext context = context("session-a", "user-a");

        assertTrue(filesystem.write(context, "folder./file.", "dot").isSuccess());
        assertTrue(filesystem.write(context, "folder /file ", "space").isSuccess());
        assertEquals("dot", filesystem.read(context, "folder./file.", 0, 0)
                .fileData().content());
        assertEquals("space", filesystem.read(context, "folder /file ", 0, 0)
                .fileData().content());
    }

    @Test
    void dotMeansSessionRootAndSafeDotSegmentsAndMixedSeparatorsRemainUsable() {
        GuardedLocalFilesystem filesystem =
                new GuardedLocalFilesystem(tempDir.resolve("workspace"), 1024);
        RuntimeContext context = context("session-a", "user-a");

        assertTrue(filesystem.ls(context, ".").isSuccess());
        assertTrue(filesystem.write(context, "nested\\dir/file.txt", "hello").isSuccess());
        assertTrue(filesystem.write(context, "safe/./other.txt", "other").isSuccess());

        ReadResult mixedRead = filesystem.read(context, "nested/dir\\file.txt", 0, 0);
        assertTrue(mixedRead.isSuccess());
        assertEquals("hello", mixedRead.fileData().content());
        LsResult listing = filesystem.ls(context, "nested/./dir");
        assertTrue(listing.isSuccess());
        assertEquals(List.of("nested/dir/file.txt"),
                listing.entries().stream().map(entry -> entry.path()).toList());
    }

    @Test
    void safelyEncodesRuntimeSessionIdAndIsolatesOnlyBySession() throws Exception {
        Path root = tempDir.resolve("workspace");
        GuardedLocalFilesystem filesystem = new GuardedLocalFilesystem(root, 1024);
        RuntimeContext first = context("../tenant/a\\C:", "user-a");
        RuntimeContext sameSessionDifferentUser = context("../tenant/a\\C:", "user-b");
        RuntimeContext second = context("../tenant/b\\C:", "user-a");

        assertTrue(filesystem.write(first, "visible.txt", "first").isSuccess());
        assertTrue(filesystem.read(sameSessionDifferentUser, "visible.txt", 0, 0).isSuccess());
        assertFalse(filesystem.read(second, "visible.txt", 0, 0).isSuccess());
        assertTrue(filesystem.write(second, "visible.txt", "second").isSuccess());

        assertEquals("first", filesystem.read(first, "visible.txt", 0, 0)
                .fileData().content());
        assertEquals("second", filesystem.read(second, "visible.txt", 0, 0)
                .fileData().content());
        assertEquals(List.of("visible.txt"), filesystem.ls(first, ".").entries().stream()
                .map(entry -> entry.path()).toList());
        assertEquals(List.of("visible.txt"), filesystem.ls(second, ".").entries().stream()
                .map(entry -> entry.path()).toList());

        List<Path> sessionRoots;
        try (Stream<Path> children = Files.list(root)) {
            sessionRoots = children.sorted().toList();
        }
        assertEquals(2, sessionRoots.size());
        assertTrue(sessionRoots.stream().allMatch(path -> path.getParent().equals(root)));
        assertTrue(sessionRoots.stream().allMatch(path -> path.getFileName().toString()
                .matches("session-[0-9a-f]{64}")));
        assertNotEquals(sessionRoots.get(0).getFileName(), sessionRoots.get(1).getFileName());

        assertThrows(
                IllegalArgumentException.class,
                () -> filesystem.ls(RuntimeContext.empty(), "."));
        assertThrows(
                IllegalArgumentException.class,
                () -> filesystem.ls(context(" ", "user-a"), "."));
        assertThrows(
                IllegalArgumentException.class,
                () -> filesystem.glob(RuntimeContext.empty(), "**/*", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> filesystem.glob(RuntimeContext.empty(), "**/*", "/"));
    }

    @Test
    void agentMemoryIsPhysicalAgentSessionStateWhileConversationFilesStayShared()
            throws Exception {
        Path root = tempDir.resolve("agent-memory-workspace");
        GuardedLocalFilesystem filesystem = new GuardedLocalFilesystem(root, 4096);
        LiteFlowAgentContext firstLiteFlow = liteFlowContext("conversation", "agent-a");
        LiteFlowAgentContext secondLiteFlow = liteFlowContext("conversation", "agent-b");
        RuntimeContext first = runtimeContext(firstLiteFlow);
        RuntimeContext second = runtimeContext(secondLiteFlow);

        assertTrue(filesystem.write(first, "MEMORY.md", "curated-a").isSuccess());
        assertTrue(filesystem.write(first, "memory/2030-01-01.md", "daily-a").isSuccess());
        assertFalse(filesystem.read(second, "MEMORY.md", 0, 0).isSuccess());
        assertFalse(filesystem.read(second, "memory/2030-01-01.md", 0, 0).isSuccess());
        assertTrue(filesystem.write(second, "MEMORY.md", "curated-b").isSuccess());
        assertTrue(filesystem.write(second, "memory/2030-01-01.md", "daily-b").isSuccess());

        ReadResult curated = filesystem.read(first, "MEMORY.md", 0, 0);
        ReadResult daily = filesystem.read(first, "memory/2030-01-01.md", 0, 0);
        assertTrue(curated.isSuccess(), curated.error());
        assertTrue(daily.isSuccess(), daily.error());
        assertEquals("curated-a", curated.fileData().content());
        assertEquals("daily-a", daily.fileData().content());
        assertEquals("curated-b", filesystem.read(second, "MEMORY.md", 0, 0)
                .fileData().content());
        assertEquals("daily-b", filesystem.read(second, "memory/2030-01-01.md", 0, 0)
                .fileData().content());

        assertTrue(filesystem.write(first, "shared.txt", "conversation").isSuccess());
        assertEquals("conversation", filesystem.read(second, "shared.txt", 0, 0)
                .fileData().content());
    }

    @Test
    void dotAliasedMemoryPathsUseTheSameAgentSessionRootAsCanonicalMemoryPaths() {
        GuardedLocalFilesystem filesystem =
                new GuardedLocalFilesystem(tempDir.resolve("aliased-memory-workspace"), 4096);
        RuntimeContext first = runtimeContext(liteFlowContext("conversation", "agent-a"));
        RuntimeContext second = runtimeContext(liteFlowContext("conversation", "agent-b"));

        assertTrue(filesystem.write(first, "./MEMORY.md", "curated-a").isSuccess());
        assertTrue(filesystem.write(first, "./memory/2030-01-01.md", "daily-a").isSuccess());

        assertEquals("curated-a", filesystem.read(first, "MEMORY.md", 0, 0)
                .fileData().content());
        assertEquals("daily-a", filesystem.read(first, "memory/2030-01-01.md", 0, 0)
                .fileData().content());
        assertFalse(filesystem.read(second, "./MEMORY.md", 0, 0).isSuccess());
        assertFalse(filesystem.read(second, "./memory/2030-01-01.md", 0, 0).isSuccess());
        assertEquals(List.of("memory/2030-01-01.md"),
                filesystem.glob(first, "*.md", "./memory").matches().stream()
                        .map(match -> match.path()).toList());
        assertEquals(List.of("memory/2030-01-01.md"),
                filesystem.ls(first, "./memory").entries().stream()
                        .map(entry -> entry.path()).toList());
    }

    @Test
    void memoryListingsAndSearchExposeOnlyVirtualPathsAndReadThosePathsSuccessfully() {
        Path root = tempDir.resolve("virtual-memory-workspace");
        GuardedLocalFilesystem filesystem = new GuardedLocalFilesystem(root, 4096);
        RuntimeContext context = runtimeContext(liteFlowContext("conversation", "agent-a"));
        assertTrue(filesystem.write(context, "MEMORY.md", "curated needle").isSuccess());
        assertTrue(filesystem.write(
                context, "memory/2030-01-01.md", "daily needle").isSuccess());

        GlobResult glob = filesystem.glob(context, "*.md", "memory");
        assertEquals(List.of("memory/2030-01-01.md"),
                glob.matches().stream().map(match -> match.path()).toList());
        assertEquals(List.of("memory/2030-01-01.md"),
                filesystem.ls(context, "memory").entries().stream()
                        .map(entry -> entry.path()).toList());

        WorkspaceManager workspace = new WorkspaceManager(root, filesystem);
        assertEquals(List.of("MEMORY.md", "memory/2030-01-01.md"),
                workspace.listMemoryFilePaths(context));
        String matches = new MemorySearchTool(workspace).memorySearch(context, "needle");
        assertTrue(matches.contains("Source: MEMORY.md#1: curated needle"), matches);
        assertTrue(matches.contains("Source: memory/2030-01-01.md#1: daily needle"), matches);
        assertFalse(matches.contains("agent-"), matches);
        assertFalse(matches.contains("session-"), matches);

        assertTrue(filesystem.write(context, "ordinary/file.txt", "ordinary").isSuccess());
        assertEquals(List.of("ordinary/file.txt"),
                filesystem.glob(context, "*.txt", "ordinary").matches().stream()
                        .map(match -> match.path()).toList());
        assertEquals(List.of("ordinary/file.txt"),
                filesystem.ls(context, "ordinary").entries().stream()
                        .map(entry -> entry.path()).toList());
    }

    @Test
    void internalAndNonLiteFlowContextsKeepTheExistingSessionMemoryLayout() {
        GuardedLocalFilesystem filesystem =
                new GuardedLocalFilesystem(tempDir.resolve("internal-memory-workspace"), 4096);
        RuntimeContext internal = context("internal-session", "internal-user");
        RuntimeContext sameSession = context("internal-session", "other-user");

        assertTrue(filesystem.write(internal, "MEMORY.md", "internal").isSuccess());
        assertTrue(filesystem.write(internal, "memory/2030-01-01.md", "daily").isSuccess());
        assertEquals("internal", filesystem.read(sameSession, "MEMORY.md", 0, 0)
                .fileData().content());
        assertEquals("daily", filesystem.read(sameSession, "memory/2030-01-01.md", 0, 0)
                .fileData().content());
    }

    @Test
    void rejectsFileAndDirectorySymlinksForReadListDeleteWriteUploadAndMove() throws Exception {
        Path root = tempDir.resolve("workspace");
        GuardedLocalFilesystem filesystem = new GuardedLocalFilesystem(root, 1024);
        RuntimeContext context = context("session-a", "user-a");
        assertTrue(filesystem.write(context, "seed.txt", "seed").isSuccess());
        Path sessionRoot = onlySessionRoot(root);

        Path outsideFile = tempDir.resolve("outside.txt");
        Files.writeString(outsideFile, "outside", StandardCharsets.UTF_8);
        Files.createSymbolicLink(sessionRoot.resolve("file-link"), outsideFile);

        assertThrows(SecurityException.class,
                () -> filesystem.read(context, "file-link", 0, 0));
        assertThrows(SecurityException.class,
                () -> filesystem.ls(context, "file-link"));
        assertThrows(SecurityException.class,
                () -> filesystem.delete(context, "file-link"));
        assertThrows(SecurityException.class,
                () -> filesystem.move(context, "file-link", "moved.txt"));
        assertEquals("outside", Files.readString(outsideFile, StandardCharsets.UTF_8));

        Path outsideDirectory = tempDir.resolve("outside-directory");
        Files.createDirectories(outsideDirectory);
        Path nested = sessionRoot.resolve("nested");
        Files.createDirectories(nested);
        Files.createSymbolicLink(nested.resolve("dir-link"), outsideDirectory);

        assertThrows(SecurityException.class,
                () -> filesystem.write(context, "nested/dir-link/write.txt", "denied"));
        FileUploadResponse upload = filesystem.uploadFiles(
                context,
                List.of(Map.entry("nested\\dir-link/upload.bin", new byte[] {1, 2, 3})))
                .get(0);
        assertFalse(upload.isSuccess());
        assertThrows(SecurityException.class,
                () -> filesystem.move(context, "seed.txt", "nested/dir-link/moved.txt"));
        assertTrue(filesystem.exists(context, "seed.txt"));
        assertFalse(Files.exists(outsideDirectory.resolve("write.txt")));
        assertFalse(Files.exists(outsideDirectory.resolve("upload.bin")));
        assertFalse(Files.exists(outsideDirectory.resolve("moved.txt")));
    }

    @Test
    void validatesBothMoveEndpointsBeforeMutation() throws Exception {
        Path root = tempDir.resolve("workspace");
        GuardedLocalFilesystem filesystem = new GuardedLocalFilesystem(root, 1024);
        RuntimeContext context = context("session-a", "user-a");
        assertTrue(filesystem.write(context, "source.txt", "source").isSuccess());

        assertThrows(SecurityException.class,
                () -> filesystem.move(context, "../source.txt", "target.txt"));
        assertThrows(SecurityException.class,
                () -> filesystem.move(context, "source.txt", "..\\target.txt"));

        assertTrue(filesystem.exists(context, "source.txt"));
        assertFalse(Files.exists(tempDir.resolve("target.txt")));
    }

    @Test
    void rejectsOversizedUtf8WriteAndBinaryUploadBeforeChangingTargets() throws Exception {
        Path root = tempDir.resolve("workspace");
        GuardedLocalFilesystem filesystem = new GuardedLocalFilesystem(root, 3);
        RuntimeContext context = context("session-a", "user-a");

        WriteResult oversizedWrite = filesystem.write(context, "utf8.txt", "你a");
        assertFalse(oversizedWrite.isSuccess());
        try (Stream<Path> children = Files.list(root)) {
            assertEquals(0, children.count());
        }

        List<FileUploadResponse> initial = filesystem.uploadFiles(
                context,
                List.of(
                        Map.entry("kept.bin", new byte[] {1, 2, 3}),
                        Map.entry("oversized.bin", new byte[] {1, 2, 3, 4})));
        assertTrue(initial.get(0).isSuccess());
        assertFalse(initial.get(1).isSuccess());
        assertFalse(filesystem.exists(context, "oversized.bin"));

        FileUploadResponse replacement = filesystem.uploadFiles(
                context,
                List.of(Map.entry("kept.bin", new byte[] {9, 9, 9, 9})))
                .get(0);
        assertFalse(replacement.isSuccess());
        FileDownloadResponse kept = filesystem.downloadFiles(context, List.of("kept.bin")).get(0);
        assertTrue(kept.isSuccess());
        assertArrayEquals(new byte[] {1, 2, 3}, kept.content());

        assertTrue(filesystem.write(context, "editable.txt", "a").isSuccess());
        EditResult oversizedEdit = filesystem.edit(context, "editable.txt", "a", "你a", false);
        assertFalse(oversizedEdit.isSuccess());
        assertEquals("a", filesystem.read(context, "editable.txt", 0, 0)
                .fileData().content());
    }

    @Test
    void pinnedUpstreamIsolatedDeclarationFallsBackToHostLocalFilesystemAndShell()
            throws Exception {
        Path root = tempDir.resolve("workspace");
        AgentConfig config = new AgentConfig();
        config.getWorkspace().setRoot(root.toString());
        HarnessFilesystemContext context =
                new HarnessFilesystemContext(root, 17, Duration.ofSeconds(2), config);
        HarnessAgent.Builder builder = HarnessAgent.builder().model(model());
        new GuardedLocalFilesystemConfigurer("lf-" + "a".repeat(64))
                .configure(builder, context);
        builder.subagent(SubagentDeclaration.builder()
                .name("unsafe-isolated")
                .description("Pinned upstream fallback proof")
                .inlineAgentsBody("isolated")
                .build());

        SubagentEntry entry = builder.buildSubagentEntries(root).stream()
                .filter(candidate -> candidate.name().equals("unsafe-isolated"))
                .findFirst()
                .orElseThrow();
        HarnessAgent child = assertInstanceOf(
                HarnessAgent.class, entry.factory().create(context("parent-session", "user-a")));
        try {
            WorkspaceManager workspaceManager =
                    (WorkspaceManager) harnessAgentField("workspaceManager").get(child);
            AbstractFilesystem childFilesystem = workspaceManager.getFilesystem();
            OverlayFilesystem overlay = assertInstanceOf(OverlayFilesystem.class, childFilesystem);
            assertInstanceOf(LocalFilesystemWithShell.class, overlay.getUpper());
            assertTrue(child.getToolkit().getToolSchemas().stream()
                    .anyMatch(schema -> ShellExecuteTool.NAME.equals(schema.getName())));
        }
        finally {
            child.close();
        }
    }

    @Test
    void configurerInjectsOnlyTheGuardedAbstractFilesystemAndHonorsAutoCreate() throws Exception {
        Path root = tempDir.resolve("workspace");
        AgentConfig config = new AgentConfig();
        config.getWorkspace().setRoot(root.toString());
        config.getWorkspace().setAutoCreate(true);
        HarnessFilesystemContext context =
                new HarnessFilesystemContext(root, 17, Duration.ofSeconds(2), config);
        HarnessAgent.Builder builder = HarnessAgent.builder();

        new GuardedLocalFilesystemConfigurer("lf-" + "a".repeat(64))
                .configure(builder, context);

        Object configured = builderField("abstractFilesystem").get(builder);
        assertInstanceOf(GuardedLocalFilesystem.class, configured);
        assertFalse(configured instanceof io.agentscope.harness.agent.filesystem.local.LocalFilesystemWithShell);
        assertNull(builderField("localFilesystemSpec").get(builder));
        assertNull(builderField("sandboxFilesystemSpec").get(builder));
        assertNull(builderField("remoteFilesystemSpec").get(builder));
        assertTrue(Files.isDirectory(root));
    }

    @Test
    void stableAgentScopedInternalSessionRecoversBusAndAsyncRecordsWithoutDirectoryGrowth()
            throws Exception {
        Path root = tempDir.resolve("stable-internal-workspace");
        AgentConfig config = new AgentConfig();
        config.getWorkspace().setRoot(root.toString());
        HarnessFilesystemContext context =
                new HarnessFilesystemContext(root, 4096, Duration.ofSeconds(2), config);
        String firstAgent = "lf-" + "a".repeat(64);
        String secondAgent = "lf-" + "b".repeat(64);

        HarnessAgent.Builder firstBuilder = HarnessAgent.builder();
        new GuardedLocalFilesystemConfigurer(firstAgent).configure(firstBuilder, context);
        MessageBus firstBus = (MessageBus) builderField("messageBus").get(firstBuilder);
        AsyncToolRegistry firstRegistry =
                (AsyncToolRegistry) builderField("asyncToolRegistry").get(firstBuilder);
        firstBus.queuePush("recoverable", Map.of("value", "before-rebuild")).block();
        AsyncToolRecord record = new AsyncToolRecord(
                "async-1",
                "conversation-1",
                "slow-tool",
                "tool-call-1",
                AsyncToolRecord.RUNNING,
                Instant.now().minusSeconds(10));
        firstRegistry.register(record).block();
        assertEquals(1, sessionRootCount(root));

        HarnessAgent.Builder rebuiltBuilder = HarnessAgent.builder();
        new GuardedLocalFilesystemConfigurer(firstAgent).configure(rebuiltBuilder, context);
        MessageBus rebuiltBus = (MessageBus) builderField("messageBus").get(rebuiltBuilder);
        AsyncToolRegistry rebuiltRegistry =
                (AsyncToolRegistry) builderField("asyncToolRegistry").get(rebuiltBuilder);

        assertEquals(
                "before-rebuild",
                rebuiltBus.queueDrain("recoverable", 10).block().get(0).payload().get("value"));
        assertEquals(
                List.of(record),
                rebuiltRegistry.findStale("conversation-1", Duration.ZERO).block());
        assertEquals(1, sessionRootCount(root));

        HarnessAgent.Builder otherBuilder = HarnessAgent.builder();
        new GuardedLocalFilesystemConfigurer(secondAgent).configure(otherBuilder, context);
        MessageBus otherBus = (MessageBus) builderField("messageBus").get(otherBuilder);
        assertFalse(otherBus.queuePeek("recoverable").block());
        otherBus.queuePush("other", Map.of("value", "second-agent")).block();
        assertEquals(2, sessionRootCount(root));

        GuardedLocalFilesystem guarded = (GuardedLocalFilesystem)
                builderField("abstractFilesystem").get(rebuiltBuilder);
        RuntimeContext ordinaryConversation = context("lf-" + "c".repeat(64), "user-a");
        assertFalse(guarded.exists(ordinaryConversation, ".agentscope/bus"));
        assertTrue(guarded.write(ordinaryConversation, "ordinary.txt", "ordinary").isSuccess());
        assertEquals(3, sessionRootCount(root));
    }

    @Test
    void disabledAutoCreateRequiresAnExistingWorkspaceRoot() {
        Path root = tempDir.resolve("missing-workspace");
        AgentConfig config = new AgentConfig();
        config.getWorkspace().setRoot(root.toString());
        config.getWorkspace().setAutoCreate(false);
        HarnessFilesystemContext context =
                new HarnessFilesystemContext(root, 17, Duration.ofSeconds(2), config);

        assertThrows(
                IllegalArgumentException.class,
                () -> new GuardedLocalFilesystemConfigurer("lf-" + "a".repeat(64))
                        .configure(HarnessAgent.builder(), context));
        assertFalse(Files.exists(root));
    }

    private static RuntimeContext context(String sessionId, String userId) {
        return RuntimeContext.builder().sessionId(sessionId).userId(userId).build();
    }

    private static LiteFlowAgentContext liteFlowContext(String conversationId, String agentKey) {
        AgentInvocationIdentity identity = new InvocationIdentityResolver("namespace")
                .resolve("user", conversationId, agentKey);
        Slot slot = new Slot();
        slot.setChainId("chain");
        slot.setConversationId(conversationId);
        slot.putRequestId("request");
        return new LiteFlowAgentContext(
                identity,
                slot,
                "chain",
                agentKey,
                "request",
                "trace",
                Instant.parse("2030-01-01T00:00:00Z"),
                AgentOutputSpec.text(),
                LiteFlowAgentContext.SLOT_ATTACHMENT_PREFIX + agentKey);
    }

    private static RuntimeContext runtimeContext(LiteFlowAgentContext context) {
        return RuntimeContext.builder()
                .userId(context.getRuntimeUserId())
                .sessionId(context.getRuntimeSessionId())
                .put(LiteFlowAgentContext.class, context)
                .build();
    }

    private static Path onlySessionRoot(Path root) throws Exception {
        try (Stream<Path> children = Files.list(root)) {
            List<Path> paths = children.toList();
            assertEquals(1, paths.size());
            return paths.get(0);
        }
    }

    private static long sessionRootCount(Path root) throws Exception {
        try (Stream<Path> children = Files.list(root)) {
            return children.filter(Files::isDirectory).count();
        }
    }

    private static Field builderField(String name) throws Exception {
        Field field = HarnessAgent.Builder.class.getDeclaredField(name);
        assertTrue(field.trySetAccessible());
        return field;
    }

    private static Field harnessAgentField(String name) throws Exception {
        Field field = HarnessAgent.class.getDeclaredField(name);
        assertTrue(field.trySetAccessible());
        return field;
    }

    private static Model model() {
        return (Model) Proxy.newProxyInstance(
                Model.class.getClassLoader(),
                new Class<?>[] {Model.class},
                (proxy, method, arguments) -> {
                    throw new AssertionError("model must not be invoked");
                });
    }
}
