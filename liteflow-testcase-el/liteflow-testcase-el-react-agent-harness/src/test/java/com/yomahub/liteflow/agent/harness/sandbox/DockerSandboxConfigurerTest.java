package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.DockerSandboxConfig;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.sandbox.ExecResult;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.SandboxContext;
import io.agentscope.harness.agent.sandbox.SandboxState;
import io.agentscope.harness.agent.sandbox.WorkspaceProjectionApplier;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandbox;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClient;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxState;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerFilesystemSpec;
import io.agentscope.harness.agent.sandbox.layout.BindMountEntry;
import io.agentscope.harness.agent.sandbox.layout.WorkspaceProjectionEntry;
import io.agentscope.harness.agent.sandbox.snapshot.LocalSnapshotSpec;
import io.agentscope.harness.agent.sandbox.snapshot.NoopSnapshotSpec;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    void disabledProjectionUsesTheUpstreamDefaultDockerClient() throws Exception {
        AgentConfig agent = agentConfig();
        agent.getHarness().getDocker().setWorkspaceProjectionEnabled(false);

        SandboxContext sandbox = configuredSpec(new DockerSandboxConfigurer(), context(agent))
                .toSandboxContext(workspace);

        assertInstanceOf(DockerSandboxClient.class, sandbox.getClient());
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

    @Test
    void rejectsImageOptionsBeforeTheUpstreamCommandCanInterpretThem() {
        AgentConfig agent = agentConfig();
        agent.getHarness().getDocker().setImage("--network=host");

        assertConfigurerRejects(agent, "liteflow.agent.harness.docker.image");
    }

    @Test
    void upstreamDockerCommandPlacesTheImageWithoutAnOptionTerminator() throws Exception {
        DockerSandboxState state = dockerState("--network=host");
        Method commandBuilder = DockerSandbox.class.getDeclaredMethod(
                "buildDockerRunCommand", String.class);
        assertTrue(commandBuilder.trySetAccessible());

        @SuppressWarnings("unchecked")
        List<String> command = (List<String>) commandBuilder.invoke(
                new DockerSandbox(state), "sandbox-test");

        assertFalse(command.contains("--"));
        assertEquals("--network=host", command.get(command.size() - 4));
        assertEquals(List.of("sh", "-c", "while :; do sleep 3600; done"),
                command.subList(command.size() - 3, command.size()));
    }

    @Test
    void upstreamProjectionReadsExternalContentThroughAFileSymlink() throws Exception {
        Path source = Files.createDirectories(workspace.resolve("source"));
        Path external = workspace.resolve("external-secret.txt");
        String secret = "outside-workspace-secret";
        Files.writeString(external, secret);
        Files.createSymbolicLink(source.resolve("AGENTS.md"), external);

        Method collector = WorkspaceProjectionApplier.class.getDeclaredMethod(
                "collectProjectedFiles", List.class);
        assertTrue(collector.trySetAccessible());
        WorkspaceProjectionEntry projection = assertInstanceOf(
                WorkspaceProjectionEntry.class,
                projectionSpec(source, "AGENTS.md")
                        .getEntries()
                        .get("__workspace_projection__"));

        @SuppressWarnings("unchecked")
        Map<String, Path> projected =
                (Map<String, Path>) collector.invoke(null, List.of(projection));

        assertEquals(1, projected.size());
        assertEquals(secret, Files.readString(projected.get("AGENTS.md")));
    }

    @Test
    void projectionPreflightRejectsLeafDirectoryIntermediateRootAndBrokenSymlinks()
            throws Exception {
        Path source = Files.createDirectories(workspace.resolve("guarded-source"));
        Path externalDirectory = Files.createDirectories(workspace.resolve("external-directory"));
        Path externalFile = externalDirectory.resolve("secret.txt");
        Files.writeString(externalFile, "external-content");

        assertProjectionSymlinkRejected(
                source,
                List.of("leaf.txt"),
                () -> Files.createSymbolicLink(source.resolve("leaf.txt"), externalFile));
        assertProjectionSymlinkRejected(
                source,
                List.of("directory"),
                () -> Files.createSymbolicLink(source.resolve("directory"), externalDirectory));

        Path parent = Files.createDirectories(source.resolve("parent"));
        assertProjectionSymlinkRejected(
                source,
                List.of("parent/intermediate/secret.txt"),
                () -> Files.createSymbolicLink(parent.resolve("intermediate"), externalDirectory));
        assertProjectionSymlinkRejected(
                source,
                List.of("broken"),
                () -> Files.createSymbolicLink(
                        source.resolve("broken"), source.resolve("missing-target")));
        assertProjectionSymlinkRejected(
                source,
                List.of("tree-file-link"),
                () -> {
                    Path tree = Files.createDirectory(source.resolve("tree-file-link"));
                    Files.createSymbolicLink(tree.resolve("leaf.txt"), externalFile);
                });
        assertProjectionSymlinkRejected(
                source,
                List.of("tree-directory-link"),
                () -> {
                    Path tree = Files.createDirectory(source.resolve("tree-directory-link"));
                    Files.createSymbolicLink(tree.resolve("nested"), externalDirectory);
                });

        Path realRoot = Files.createDirectories(workspace.resolve("real-root"));
        Path linkedRoot = workspace.resolve("linked-root");
        Files.createSymbolicLink(linkedRoot, realRoot);
        AgentConfig rootAgent = agentConfig();
        rootAgent.getWorkspace().setRoot(linkedRoot.toString());
        rootAgent.getHarness().getDocker().setWorkspaceProjectionRoots(List.of("."));
        Runnable rootPreflight = DockerSandboxConfigurer.workspaceProjectionPreflight(
                new HarnessFilesystemContext(
                        linkedRoot, 1024L, Duration.ofSeconds(5), rootAgent));

        AgentConfigException rootFailure = assertThrows(
                AgentConfigException.class, rootPreflight::run);
        assertTrue(rootFailure.getMessage().contains("symbolic link"));
    }

    @Test
    void projectionPreflightObservesChangesMadeAfterRuntimeConstruction() throws Exception {
        Path source = Files.createDirectories(workspace.resolve("late-change-source"));
        AgentConfig agent = agentConfig();
        agent.getWorkspace().setRoot(source.toString());
        agent.getHarness().getDocker().setWorkspaceProjectionRoots(List.of("AGENTS.md"));
        Runnable preflight = DockerSandboxConfigurer.workspaceProjectionPreflight(
                new HarnessFilesystemContext(source, 1024L, Duration.ofSeconds(5), agent));

        Path external = workspace.resolve("late-external.txt");
        Files.writeString(external, "late external content");
        Files.createSymbolicLink(source.resolve("AGENTS.md"), external);

        AtomicBoolean upstreamReadReached = new AtomicBoolean();
        AgentConfigException failure = assertThrows(AgentConfigException.class, () -> {
            preflight.run();
            upstreamReadReached.set(true);
            WorkspaceProjectionApplier.build(projectionSpec(source, "AGENTS.md"));
        });
        assertTrue(failure.getMessage().contains("symbolic link"));
        assertFalse(upstreamReadReached.get());
    }

    @Test
    void disabledProjectionDoesNotInspectTheHostWorkspaceTree() throws Exception {
        Path realRoot = Files.createDirectories(workspace.resolve("disabled-real-root"));
        Path linkedRoot = workspace.resolve("disabled-linked-root");
        Files.createSymbolicLink(linkedRoot, realRoot);
        AgentConfig agent = agentConfig();
        agent.getWorkspace().setRoot(linkedRoot.toString());
        agent.getHarness().getDocker().setWorkspaceProjectionEnabled(false);
        Runnable preflight = DockerSandboxConfigurer.workspaceProjectionPreflight(
                new HarnessFilesystemContext(
                        linkedRoot, 1024L, Duration.ofSeconds(5), agent));

        preflight.run();
    }

    @Test
    void projectionIsValidatedAfterSkillRepositoryBeforeStartCallbacks() throws Exception {
        Path source = Files.createDirectories(workspace.resolve("callback-source"));
        Path external = workspace.resolve("callback-external.txt");
        String secret = "callback-created-external-content";
        Files.writeString(external, secret);
        AgentConfig agentConfig = agentConfig();
        agentConfig.getWorkspace().setRoot(source.toString());
        agentConfig.getHarness().getDocker().setWorkspaceProjectionRoots(
                List.of(".skills-cache"));
        HarnessFilesystemContext filesystemContext = new HarnessFilesystemContext(
                source, 1024L, Duration.ofSeconds(5), agentConfig);
        RecordingProjectionSandboxClient client = new RecordingProjectionSandboxClient();
        DockerSandboxConfigurer configurer = new DockerSandboxConfigurer(null, client);
        HarnessAgent.Builder builder = HarnessAgent.builder()
                .name("callback-agent")
                .agentId("callback-agent")
                .model(new StaticModel())
                .stateStore(new InMemoryAgentStateStore())
                .workspace(source)
                .skillRepository(new LinkingSkillRepository(source, external))
                .disableDefaultWorkspaceSkills()
                .disableSubagents()
                .disableCompaction()
                .disableToolResultEviction()
                .disableMemoryTools()
                .disableMemoryHooks()
                .disableWorkspaceContext()
                .disableAtPathExpansion()
                .disableToolsConfig()
                .disableFilesystemTools()
                .disableShellTool();
        configurer.configure(builder, filesystemContext);
        HarnessAgent agent = builder.build();

        try {
            for (int call = 0; call < 2; call++) {
                configurer.workspaceProjectionPreflight(filesystemContext).run();
                RuntimeException failure = null;
                try {
                    agent.call(
                                    List.of(new UserMessage("question")),
                                    RuntimeContext.builder()
                                            .userId("user")
                                            .sessionId("session-" + call)
                                            .build())
                            .block();
                }
                catch (RuntimeException expected) {
                    failure = expected;
                }
                assertTrue(hasCause(failure, AgentConfigException.class));
                assertFalse(
                        client.projectionRead,
                        "callback ran after call preflight and projection read: "
                                + client.projectedContent);
                Files.delete(source.resolve(".skills-cache/repository/external.txt"));
            }
        }
        finally {
            agent.close();
        }

        assertEquals(2, client.createdSandboxes);
    }

    private AgentConfig agentConfig() {
        AgentConfig agent = new AgentConfig();
        agent.getStateStore().setJsonRoot("target/agent-state");
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

    private static DockerSandboxState dockerState(String image) {
        DockerSandboxState state = new DockerSandboxState();
        state.setSessionId("session");
        state.setWorkspaceSpec(new WorkspaceSpec());
        state.setImage(image);
        state.setWorkspaceRoot("/workspace");
        state.setNetwork("none");
        state.setAdditionalRunArgs(REQUIRED_RUN_ARGS);
        return state;
    }

    private static WorkspaceSpec projectionSpec(Path source, String root) {
        WorkspaceProjectionEntry projection = new WorkspaceProjectionEntry();
        projection.setSourceRoot(source.toString());
        projection.setIncludeRoots(List.of(root));
        WorkspaceSpec spec = new WorkspaceSpec();
        spec.getEntries().put("__workspace_projection__", projection);
        return spec;
    }

    private static void assertProjectionSymlinkRejected(
            Path source, List<String> roots, ThrowingAction createSymlink) throws Exception {
        createSymlink.run();
        AgentConfig agent = new AgentConfig();
        agent.getWorkspace().setRoot(source.toString());
        agent.getHarness().getDocker().setWorkspaceProjectionRoots(roots);
        Runnable preflight = DockerSandboxConfigurer.workspaceProjectionPreflight(
                new HarnessFilesystemContext(
                        source, 1024L, Duration.ofSeconds(5), agent));

        AgentConfigException failure = assertThrows(AgentConfigException.class, preflight::run);
        assertTrue(failure.getMessage().contains("symbolic link"));
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private static final class LinkingSkillRepository implements AgentSkillRepository {
        private final Path workspace;
        private final Path external;

        private LinkingSkillRepository(Path workspace, Path external) {
            this.workspace = workspace;
            this.external = external;
        }

        @Override
        public List<AgentSkill> getAllSkills() {
            try {
                Path cache = Files.createDirectories(workspace.resolve(".skills-cache/repository"));
                Path link = cache.resolve("external.txt");
                if (!Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createSymbolicLink(link, external);
                }
                return List.of();
            }
            catch (Exception failure) {
                throw new IllegalStateException(failure);
            }
        }

        @Override
        public AgentSkill getSkill(String name) {
            return null;
        }

        @Override
        public List<String> getAllSkillNames() {
            return List.of();
        }

        @Override
        public boolean save(List<AgentSkill> skills, boolean force) {
            return false;
        }

        @Override
        public boolean delete(String skillName) {
            return false;
        }

        @Override
        public boolean skillExists(String skillName) {
            return false;
        }

        @Override
        public AgentSkillRepositoryInfo getRepositoryInfo() {
            return new AgentSkillRepositoryInfo("test", "linking", false);
        }

        @Override
        public String getSource() {
            return "linking";
        }

        @Override
        public void setWriteable(boolean writeable) {
        }

        @Override
        public boolean isWriteable() {
            return false;
        }
    }

    private static final class RecordingProjectionSandboxClient
            implements SandboxClient<DockerSandboxClientOptions> {
        private int createdSandboxes;
        private boolean projectionRead;
        private String projectedContent;

        @Override
        public Sandbox create(
                WorkspaceSpec workspaceSpec,
                SandboxSnapshotSpec snapshotSpec,
                DockerSandboxClientOptions options) {
            createdSandboxes++;
            return new RecordingProjectionSandbox(this, workspaceSpec);
        }

        @Override
        public Sandbox resume(SandboxState state) {
            return new RecordingProjectionSandbox(this, state.getWorkspaceSpec());
        }

        @Override
        public void delete(Sandbox sandbox) {
        }

        @Override
        public String serializeState(SandboxState state) {
            return "{}";
        }

        @Override
        public SandboxState deserializeState(String json) {
            throw new AssertionError("state restore is not expected");
        }
    }

    private static final class RecordingProjectionSandbox implements Sandbox {
        private final RecordingProjectionSandboxClient client;
        private final SandboxState state = new TestSandboxState();

        private RecordingProjectionSandbox(
                RecordingProjectionSandboxClient client, WorkspaceSpec workspaceSpec) {
            this.client = client;
            state.setWorkspaceSpec(workspaceSpec);
            state.setSessionId("recording-session");
        }

        @Override
        public void start() throws Exception {
            WorkspaceProjectionEntry projection = assertInstanceOf(
                    WorkspaceProjectionEntry.class,
                    state.getWorkspaceSpec().getEntries().get("__workspace_projection__"));
            Method collector = WorkspaceProjectionApplier.class.getDeclaredMethod(
                    "collectProjectedFiles", List.class);
            assertTrue(collector.trySetAccessible());
            @SuppressWarnings("unchecked")
            Map<String, Path> projected = (Map<String, Path>) collector.invoke(
                    null, List.of(projection));
            Path first = projected.values().stream().findFirst().orElseThrow();
            client.projectedContent = Files.readString(first);
            client.projectionRead = true;
        }

        @Override
        public void stop() {
        }

        @Override
        public void shutdown() {
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isRunning() {
            return true;
        }

        @Override
        public SandboxState getState() {
            return state;
        }

        @Override
        public ExecResult exec(
                RuntimeContext context, String command, Integer timeoutSeconds) {
            throw new AssertionError("sandbox exec is not expected");
        }

        @Override
        public InputStream persistWorkspace() {
            throw new AssertionError("sandbox persistence is not expected");
        }

        @Override
        public void hydrateWorkspace(InputStream archive) {
            throw new AssertionError("sandbox hydration is not expected");
        }
    }

    private static final class TestSandboxState extends SandboxState {
    }

    private static final class StaticModel implements Model {
        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            ContentBlock content = TextBlock.builder().text("reply").build();
            return Flux.just(ChatResponse.builder()
                    .content(List.of(content))
                    .finishReason("stop")
                    .build());
        }

        @Override
        public String getModelName() {
            return "static";
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
