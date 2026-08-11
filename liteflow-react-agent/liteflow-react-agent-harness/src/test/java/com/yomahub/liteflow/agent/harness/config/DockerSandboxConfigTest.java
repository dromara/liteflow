package com.yomahub.liteflow.agent.harness.config;

import com.yomahub.liteflow.property.agent.DockerSandboxConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerSandboxConfigTest {

    private static final List<String> OFFICIAL_PROJECTION_ROOTS = List.of(
            "AGENTS.md", "skills", "subagents", "knowledge", ".skills-cache");

    @Test
    void defaultsApplyResourceIsolationAndOfficialWorkspaceProjection() {
        DockerSandboxConfig config = new DockerSandboxConfig();

        assertEquals("ubuntu:22.04", config.getImage());
        assertEquals("/workspace", config.getWorkspaceRoot());
        assertEquals(536870912L, config.getMemorySizeBytes());
        assertEquals(1L, config.getCpuCount());
        assertEquals(Long.class, getterType("getCpuCount"));
        assertEquals("none", config.getNetwork());
        assertNull(config.getSnapshotRoot());
        assertTrue(config.isWorkspaceProjectionEnabled());
        assertEquals(OFFICIAL_PROJECTION_ROOTS, config.getWorkspaceProjectionRoots());
        assertDoesNotThrow(config::validate);
    }

    @Test
    void nonPositiveOrMissingCpuQuotaFailsFast() {
        for (Long invalid : new Long[] {null, 0L, -1L}) {
            DockerSandboxConfig config = new DockerSandboxConfig();
            config.setCpuCount(invalid);

            IllegalStateException failure = assertThrows(
                    IllegalStateException.class, config::validate);

            assertTrue(failure.getMessage().contains("cpu-count"));
        }
    }

    @Test
    void nonPositiveOrMissingMemoryQuotaFailsFast() {
        for (Long invalid : new Long[] {null, 0L, -1L}) {
            DockerSandboxConfig config = new DockerSandboxConfig();
            config.setMemorySizeBytes(invalid);

            IllegalStateException failure = assertThrows(
                    IllegalStateException.class, config::validate);

            assertTrue(failure.getMessage().contains("memory-size-bytes"));
        }
    }

    @Test
    void blankImageWorkspaceRootOrNetworkFailsFast() {
        DockerSandboxConfig blankImage = new DockerSandboxConfig();
        blankImage.setImage("  ");
        assertValidationMentions(blankImage, "image");

        DockerSandboxConfig blankWorkspace = new DockerSandboxConfig();
        blankWorkspace.setWorkspaceRoot("");
        assertValidationMentions(blankWorkspace, "workspace-root");

        DockerSandboxConfig blankNetwork = new DockerSandboxConfig();
        blankNetwork.setNetwork("\t");
        assertValidationMentions(blankNetwork, "network");
    }

    @Test
    void projectionRootsRejectParentTraversalSegments() {
        for (String invalid : List.of("..", "skills/../secret", "skills\\..\\secret")) {
            DockerSandboxConfig config = new DockerSandboxConfig();
            config.setWorkspaceProjectionRoots(List.of(invalid));

            IllegalStateException failure = assertThrows(
                    IllegalStateException.class, config::validate);

            assertTrue(failure.getMessage().contains("workspace-projection-roots"));
        }
    }

    @Test
    void projectionRootsRejectPosixAbsolutePaths() {
        for (String invalid : List.of("/etc", "//server/share")) {
            assertProjectionRootRejected(invalid);
        }
    }

    @Test
    void projectionRootsRejectWindowsDrivePrefixesOnEveryHost() {
        for (String invalid : List.of("C:\\sensitive", "D:/sensitive", "C:sensitive")) {
            assertProjectionRootRejected(invalid);
        }
    }

    @Test
    void projectionRootsRejectUncAndRootedWindowsPathsOnEveryHost() {
        for (String invalid : List.of("\\\\server\\share", "\\rooted")) {
            assertProjectionRootRejected(invalid);
        }
    }

    @Test
    void projectionRootsRejectBlankPaths() {
        for (String invalid : List.of("", "   ", "\t")) {
            assertProjectionRootRejected(invalid);
        }
    }

    @Test
    void currentDirectoryProjectionRootsRemainAcceptedAndUnchanged() {
        List<String> roots = List.of(".", "./.", ".\\.", "./skills/.");
        DockerSandboxConfig config = new DockerSandboxConfig();
        config.setWorkspaceProjectionRoots(roots);

        assertDoesNotThrow(config::validate);
        assertEquals(roots, config.getWorkspaceProjectionRoots());
    }

    @Test
    void projectionRootsRejectSpecialSegmentsAfterWin32TrailingCharacterStripping() {
        for (String invalid : List.of(".. ", "...", ". ", ".   ")) {
            assertProjectionRootRejected(invalid);
        }
    }

    @Test
    void nestedWin32StrippedTraversalIsRejectedAcrossSeparatorStyles() {
        for (String invalid : List.of(
                "skills\\.. \\secret", "skills/.. \\secret", "skills\\.. /secret")) {
            assertProjectionRootRejected(invalid);
        }
    }

    @Test
    void projectionRootsRejectSegmentsEntirelyStrippedByWin32() {
        for (String invalid : List.of(
                "skills/   /secret", "skills/. . /secret", "skills\\...   \\secret")) {
            assertProjectionRootRejected(invalid);
        }
    }

    @Test
    void ordinaryRelativeSegmentsWithSpacesAndDotsRemainAcceptedAndUnchanged() {
        List<String> roots = List.of(
                "folder name/file.name", "release.v1/docs", "folder./child", "folder /child");
        DockerSandboxConfig config = new DockerSandboxConfig();
        config.setWorkspaceProjectionRoots(roots);

        assertDoesNotThrow(config::validate);
        assertEquals(roots, config.getWorkspaceProjectionRoots());
    }

    @Test
    void nestedRelativeProjectionRootsRemainAcceptedAndUnchanged() {
        List<String> roots = List.of(
                "AGENTS.md", ".skills-cache", "skills/foo", "skills/./bar", "skills\\.\\baz");
        DockerSandboxConfig config = new DockerSandboxConfig();
        config.setWorkspaceProjectionRoots(roots);

        assertDoesNotThrow(config::validate);
        assertEquals(roots, config.getWorkspaceProjectionRoots());
    }

    @Test
    void explicitDockerConfigurationPreservesEveryBoundField() {
        DockerSandboxConfig config = new DockerSandboxConfig();
        config.setImage("alpine:3.20");
        config.setWorkspaceRoot("/workspace");
        config.setMemorySizeBytes(268435456L);
        config.setCpuCount(1L);
        config.setNetwork("none");
        config.setSnapshotRoot("./data/agent-snapshots");
        config.setWorkspaceProjectionEnabled(true);
        config.setWorkspaceProjectionRoots(OFFICIAL_PROJECTION_ROOTS);

        assertDoesNotThrow(config::validate);
        assertEquals("alpine:3.20", config.getImage());
        assertEquals("/workspace", config.getWorkspaceRoot());
        assertEquals(268435456L, config.getMemorySizeBytes());
        assertEquals(1L, config.getCpuCount());
        assertEquals("none", config.getNetwork());
        assertEquals("./data/agent-snapshots", config.getSnapshotRoot());
        assertTrue(config.isWorkspaceProjectionEnabled());
        assertEquals(OFFICIAL_PROJECTION_ROOTS, config.getWorkspaceProjectionRoots());
    }

    private static Class<?> getterType(String methodName) {
        try {
            return DockerSandboxConfig.class.getMethod(methodName).getReturnType();
        } catch (NoSuchMethodException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void assertValidationMentions(DockerSandboxConfig config, String property) {
        IllegalStateException failure = assertThrows(IllegalStateException.class, config::validate);
        assertTrue(failure.getMessage().contains(property));
    }

    private static void assertProjectionRootRejected(String root) {
        DockerSandboxConfig config = new DockerSandboxConfig();
        config.setWorkspaceProjectionRoots(List.of(root));

        IllegalStateException failure = assertThrows(
                IllegalStateException.class, config::validate, root);

        assertTrue(failure.getMessage().contains("workspace-projection-roots"), root);
    }
}
