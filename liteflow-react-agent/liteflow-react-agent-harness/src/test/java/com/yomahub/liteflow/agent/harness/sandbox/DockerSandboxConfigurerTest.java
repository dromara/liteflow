package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.DockerSandboxConfig;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.sandbox.SandboxContext;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerFilesystemSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.sandbox.layout.BindMountEntry;
import io.agentscope.harness.agent.sandbox.layout.WorkspaceProjectionEntry;
import io.agentscope.harness.agent.sandbox.snapshot.LocalSnapshotSpec;
import io.agentscope.harness.agent.sandbox.snapshot.NoopSnapshotSpec;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerSandboxConfigurerTest {

    private static final List<String> REQUIRED_RUN_ARGS = List.of(
            "--cap-drop=ALL",
            "--security-opt=no-new-privileges:true",
            "--pids-limit=64");

    @TempDir
    Path workspace;

    @Test
    void mapsPolicyBoundOptionsAndWorkspaceProjectionIntoTheRealSandboxContext()
            throws Exception {
        AgentConfig agent = agentConfig();
        DockerSandboxConfig docker = agent.getHarness().getDocker();
        docker.setImage("alpine:3.20");
        docker.setWorkspaceRoot("/agent-workspace");
        docker.setMemorySizeBytes(268435456L);
        docker.setCpuCount(2L);
        docker.setNetwork("none");
        docker.setSnapshotRoot(null);
        docker.setWorkspaceProjectionRoots(List.of("AGENTS.md", "skills/runtime"));

        DockerFilesystemSpec spec = configuredSpec(
                new DockerSandboxConfigurer(), context(agent));
        SandboxContext sandbox = spec.toSandboxContext(workspace);
        DockerSandboxClientOptions options = assertInstanceOf(
                DockerSandboxClientOptions.class, sandbox.getClientOptions());

        assertEquals("alpine:3.20", options.getImage());
        assertEquals("/agent-workspace", options.getWorkspaceRoot());
        assertEquals(268435456L, options.getMemorySizeBytes());
        assertEquals(2L, options.getCpuCount());
        assertEquals("none", options.getNetwork());
        assertEquals(IsolationScope.SESSION, sandbox.getIsolationScope());
        assertInstanceOf(NoopSnapshotSpec.class, sandbox.getSnapshotSpec());
        assertEquals(REQUIRED_RUN_ARGS, options.getAdditionalRunArgs());

        assertArrayEquals(new int[0], options.getExposedPorts());
        assertEquals(Map.of(), options.getEnvironment());
        assertFalse(sandbox.getWorkspaceSpec().getEntries().values().stream()
                .anyMatch(BindMountEntry.class::isInstance));
        WorkspaceProjectionEntry projection = assertInstanceOf(
                WorkspaceProjectionEntry.class,
                sandbox.getWorkspaceSpec().getEntries().get("__workspace_projection__"));
        assertEquals(workspace.toAbsolutePath().normalize().toString(), projection.getSourceRoot());
        assertEquals(List.of("AGENTS.md", "skills/runtime"), projection.getIncludeRoots());
    }

    @Test
    void mapsOnlyAnExplicitNonDefaultNetworkValue() throws Exception {
        AgentConfig defaultAgent = agentConfig();
        DockerSandboxClientOptions defaultOptions = options(configuredSpec(
                new DockerSandboxConfigurer(), context(defaultAgent)));
        assertEquals("none", defaultOptions.getNetwork());

        AgentConfig explicitAgent = agentConfig();
        explicitAgent.getHarness().getDocker().setNetwork("sandbox-network");
        DockerSandboxClientOptions explicitOptions = options(configuredSpec(
                new DockerSandboxConfigurer(), context(explicitAgent)));
        assertEquals("sandbox-network", explicitOptions.getNetwork());
    }

    @Test
    void disablesHostWorkspaceProjectionWithoutAddingBindMounts() throws Exception {
        AgentConfig agent = agentConfig();
        agent.getHarness().getDocker().setWorkspaceProjectionEnabled(false);

        SandboxContext sandbox = configuredSpec(new DockerSandboxConfigurer(), context(agent))
                .toSandboxContext(workspace);

        assertTrue(sandbox.getWorkspaceSpec().getEntries().isEmpty());
    }

    @Test
    void usesLocalSnapshotsForNonBlankRootsAndNoopSnapshotsForBlankRoots()
            throws Exception {
        AgentConfig localAgent = agentConfig();
        localAgent.getHarness().getDocker().setSnapshotRoot("snapshots/local");
        SandboxContext local = configuredSpec(
                        new DockerSandboxConfigurer(), context(localAgent))
                .toSandboxContext(workspace);
        LocalSnapshotSpec localSnapshot = assertInstanceOf(
                LocalSnapshotSpec.class, local.getSnapshotSpec());
        assertEquals(Path.of("snapshots/local").toString(), localSnapshot.getBasePath());

        for (String blank : new String[] {null, "", "  ", "\t"}) {
            AgentConfig noopAgent = agentConfig();
            noopAgent.getHarness().getDocker().setSnapshotRoot(blank);
            SandboxContext noop = configuredSpec(
                            new DockerSandboxConfigurer(), context(noopAgent))
                    .toSandboxContext(workspace);
            assertInstanceOf(NoopSnapshotSpec.class, noop.getSnapshotSpec());
        }
    }

    @Test
    void usesProviderSnapshotsWithoutExposingDockerOptionsToTheProvider() throws Exception {
        AgentConfig agent = agentConfig();
        SandboxSnapshotSpec remote = snapshotId -> null;
        HarnessFilesystemContext filesystemContext = context(agent);
        SandboxSnapshotProvider provider = received -> {
            assertSame(filesystemContext, received);
            return remote;
        };

        SandboxContext sandbox = configuredSpec(
                        new DockerSandboxConfigurer(provider), filesystemContext)
                .toSandboxContext(workspace);

        assertSame(remote, sandbox.getSnapshotSpec());
        assertEquals(1, SandboxSnapshotProvider.class.getDeclaredMethods().length);
        assertEquals(
                HarnessFilesystemContext.class,
                SandboxSnapshotProvider.class.getDeclaredMethods()[0].getParameterTypes()[0]);
        assertEquals(
                SandboxSnapshotSpec.class,
                SandboxSnapshotProvider.class.getDeclaredMethods()[0].getReturnType());
    }

    @Test
    void rejectsLocalAndProviderSnapshotsInsteadOfSilentlyOverridingEither() {
        AgentConfig agent = agentConfig();
        agent.getHarness().getDocker().setSnapshotRoot("snapshots/local");
        SandboxSnapshotProvider provider = ignored -> snapshotId -> null;

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> new DockerSandboxConfigurer(provider)
                        .configure(HarnessAgent.builder(), context(agent)));

        assertTrue(failure.getMessage().contains("snapshot-root"));
        assertTrue(failure.getMessage().contains("SandboxSnapshotProvider"));
    }

    @Test
    void rejectsNullProviderSnapshotsBeforeConstructingDockerOptions() {
        AgentConfig agent = agentConfig();

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> new DockerSandboxConfigurer(ignored -> null)
                        .configure(HarnessAgent.builder(), context(agent)));

        assertTrue(failure.getMessage().contains("must not return null"));
    }

    @Test
    void repeatsQuotaAndRequiredTextValidationBeforeConstructingTheSpec() {
        for (Long invalid : new Long[] {null, 0L, -1L}) {
            AgentConfig memoryAgent = agentConfig();
            memoryAgent.getHarness().getDocker().setMemorySizeBytes(invalid);
            assertConfigurerRejects(memoryAgent, "memory-size-bytes");

            AgentConfig cpuAgent = agentConfig();
            cpuAgent.getHarness().getDocker().setCpuCount(invalid);
            assertConfigurerRejects(cpuAgent, "cpu-count");
        }

        AgentConfig imageAgent = agentConfig();
        imageAgent.getHarness().getDocker().setImage(" ");
        assertConfigurerRejects(imageAgent, "image");

        AgentConfig workspaceAgent = agentConfig();
        workspaceAgent.getHarness().getDocker().setWorkspaceRoot("\t");
        assertConfigurerRejects(workspaceAgent, "workspace-root");

        AgentConfig networkAgent = agentConfig();
        networkAgent.getHarness().getDocker().setNetwork("");
        assertConfigurerRejects(networkAgent, "network");
    }

    private AgentConfig agentConfig() {
        AgentConfig agent = new AgentConfig();
        agent.getWorkspace().setRoot(workspace.toString());
        return agent;
    }

    private HarnessFilesystemContext context(AgentConfig agent) {
        return new HarnessFilesystemContext(
                workspace, 1024L, Duration.ofSeconds(5), agent);
    }

    private DockerSandboxClientOptions options(DockerFilesystemSpec spec) {
        return assertInstanceOf(
                DockerSandboxClientOptions.class,
                spec.toSandboxContext(workspace).getClientOptions());
    }

    private static DockerFilesystemSpec configuredSpec(
            DockerSandboxConfigurer configurer,
            HarnessFilesystemContext context) throws Exception {
        HarnessAgent.Builder builder = HarnessAgent.builder();
        configurer.configure(builder, context);
        Field field = HarnessAgent.Builder.class.getDeclaredField("sandboxFilesystemSpec");
        assertTrue(field.trySetAccessible());
        return assertInstanceOf(DockerFilesystemSpec.class, field.get(builder));
    }

    private void assertConfigurerRejects(AgentConfig agent, String property) {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> new DockerSandboxConfigurer()
                        .configure(HarnessAgent.builder(), context(agent)));
        assertTrue(failure.getMessage().contains(property));
    }
}
