package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.sandbox.SandboxContext;
import io.agentscope.harness.agent.sandbox.WorkspaceProjectionApplier;
import io.agentscope.harness.agent.sandbox.layout.WorkspaceProjectionEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceProjectionTest {

    private static final List<String> DEFAULT_ROOTS = List.of(
            "AGENTS.md", "skills", "subagents", "knowledge", ".skills-cache");

    @TempDir
    Path tempDir;

    @Test
    void dockerSpecProjectsOnlyTheFivePolicyRootsByDefault() throws Exception {
        Path workspace = Files.createDirectories(tempDir.resolve("default-roots"));
        AgentConfig config = config(workspace);
        HarnessAgent.Builder builder = HarnessAgent.builder();
        new DockerSandboxConfigurer().configure(builder, context(config, workspace));

        SandboxContext sandbox = configuredContext(builder, workspace);
        WorkspaceProjectionEntry projection = assertInstanceOf(
                WorkspaceProjectionEntry.class,
                sandbox.getWorkspaceSpec().getEntries().get("__workspace_projection__"));

        assertEquals(DEFAULT_ROOTS, projection.getIncludeRoots());
        assertEquals(1, sandbox.getWorkspaceSpec().getEntries().size());
    }

    @Test
    void upstreamProjectionArchiveAndHashAreDeterministicAndPathSorted() throws Exception {
        Path first = Files.createDirectories(tempDir.resolve("first"));
        Path second = Files.createDirectories(tempDir.resolve("second"));
        materializeSameProjectionInDifferentCreationOrder(first, false);
        materializeSameProjectionInDifferentCreationOrder(second, true);

        WorkspaceProjectionApplier.ProjectionPayload firstPayload = payload(first);
        WorkspaceProjectionApplier.ProjectionPayload secondPayload = payload(second);

        assertEquals(firstPayload.hash(), secondPayload.hash());
        assertEquals(5, firstPayload.fileCount());
        assertEquals(
                List.of(
                        ".skills-cache/cache.txt",
                        "AGENTS.md",
                        "knowledge/k.txt",
                        "skills/s.txt",
                        "subagents/a.txt"),
                tarPaths(firstPayload.tarBytes()));
    }

    @Test
    void taskTwoConfigRejectsTraversalWithoutDuplicatingProjectionValidation() {
        AgentConfig config = config(tempDir);
        config.getHarness().getDocker().setWorkspaceProjectionRoots(
                List.of("skills", "knowledge/../outside"));

        IllegalStateException failure = assertThrows(
                IllegalStateException.class, config.getHarness().getDocker()::validate);

        assertTrue(failure.getMessage().contains("'..'"));
    }

    @Test
    void taskFivePreflightRejectsSymlinksBeforeTheUpstreamCollectorReadsThem()
            throws Exception {
        Path workspace = Files.createDirectories(tempDir.resolve("symlink"));
        Path outside = tempDir.resolve("outside.txt");
        Files.writeString(outside, "outside");
        Files.createSymbolicLink(workspace.resolve("AGENTS.md"), outside);
        AgentConfig config = config(workspace);

        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                DockerSandboxConfigurer.workspaceProjectionPreflight(
                        context(config, workspace))::run);

        assertTrue(failure.getMessage().contains("symbolic link"));
    }

    private WorkspaceProjectionApplier.ProjectionPayload payload(Path workspace)
            throws Exception {
        AgentConfig config = config(workspace);
        HarnessAgent.Builder builder = HarnessAgent.builder();
        new DockerSandboxConfigurer().configure(builder, context(config, workspace));
        DockerSandboxConfigurer.workspaceProjectionPreflight(context(config, workspace)).run();
        return WorkspaceProjectionApplier.build(
                configuredContext(builder, workspace).getWorkspaceSpec());
    }

    private static void materializeSameProjectionInDifferentCreationOrder(
            Path root, boolean reverse) throws Exception {
        List<PathAndContent> files = List.of(
                new PathAndContent("subagents/a.txt", "a"),
                new PathAndContent("skills/s.txt", "s"),
                new PathAndContent("knowledge/k.txt", "k"),
                new PathAndContent("AGENTS.md", "agents"),
                new PathAndContent(".skills-cache/cache.txt", "cache"),
                new PathAndContent("ignored.txt", "ignored"));
        List<PathAndContent> ordered = new ArrayList<>(files);
        if (reverse) {
            java.util.Collections.reverse(ordered);
        }
        for (PathAndContent file : ordered) {
            Path path = root.resolve(file.path());
            Files.createDirectories(path.getParent());
            Files.writeString(path, file.content());
        }
    }

    private static List<String> tarPaths(byte[] archive) throws Exception {
        List<String> paths = new ArrayList<>();
        try (TarArchiveInputStream tar = new TarArchiveInputStream(
                new ByteArrayInputStream(archive))) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                paths.add(entry.getName());
            }
        }
        return paths;
    }

    private static AgentConfig config(Path workspace) {
        AgentConfig config = new AgentConfig();
        config.getWorkspace().setRoot(workspace.toString());
        return config;
    }

    private static HarnessFilesystemContext context(AgentConfig config, Path workspace) {
        return new HarnessFilesystemContext(
                workspace, 1024L, Duration.ofSeconds(5), config);
    }

    private static SandboxContext configuredContext(
            HarnessAgent.Builder builder, Path workspace) throws Exception {
        Field field = HarnessAgent.Builder.class.getDeclaredField("sandboxFilesystemSpec");
        assertTrue(field.trySetAccessible());
        Object spec = field.get(builder);
        return ((io.agentscope.harness.agent.filesystem.spec.SandboxFilesystemSpec) spec)
                .toSandboxContext(workspace);
    }

    private record PathAndContent(String path, String content) {
    }
}
