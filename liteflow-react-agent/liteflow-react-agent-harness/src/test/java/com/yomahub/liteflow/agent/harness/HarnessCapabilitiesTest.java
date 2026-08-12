package com.yomahub.liteflow.agent.harness;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillFilter;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.model.EditResult;
import io.agentscope.harness.agent.filesystem.model.FileData;
import io.agentscope.harness.agent.filesystem.model.FileDownloadResponse;
import io.agentscope.harness.agent.filesystem.model.FileUploadResponse;
import io.agentscope.harness.agent.filesystem.model.GlobResult;
import io.agentscope.harness.agent.filesystem.model.GrepResult;
import io.agentscope.harness.agent.filesystem.model.LsResult;
import io.agentscope.harness.agent.filesystem.model.ReadResult;
import io.agentscope.harness.agent.filesystem.model.WriteResult;
import io.agentscope.harness.agent.memory.MemoryConfig;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.harness.agent.memory.compaction.ToolResultEvictionConfig;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.subagent.task.BackgroundTask;
import io.agentscope.harness.agent.subagent.task.TaskRepository;
import io.agentscope.harness.agent.subagent.task.TaskRunSpec;
import io.agentscope.harness.agent.subagent.task.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessCapabilitiesTest {

    @TempDir
    Path tempDir;

    private final List<TestComponent> components = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        components.forEach(TestComponent::close);
        LiteflowConfigGetter.clean();
    }

    @Test
    void additionalContextFilesReachTheRealModelInDeclaredOrder() throws Exception {
        configureCustom("additional-context");
        RecordingFilesystem filesystem = new RecordingFilesystem(Map.of(
                "context/first.md", "FIRST-CONTEXT",
                "context/second.md", "SECOND-CONTEXT"));
        RecordingModel model = new RecordingModel();
        TestComponent component = component(model, filesystem);
        component.additionalContextFiles = List.of("context/first.md", "context/second.md");

        component.process();

        String prompt = model.systemText();
        assertTrue(prompt.contains("FIRST-CONTEXT"), prompt);
        assertTrue(prompt.contains("SECOND-CONTEXT"), prompt);
        assertTrue(prompt.indexOf("FIRST-CONTEXT") < prompt.indexOf("SECOND-CONTEXT"), prompt);
    }

    @ParameterizedTest
    @MethodSource("invalidAdditionalContextFiles")
    void invalidOrDuplicateAdditionalContextFilesFailBeforeModelInvocation(List<String> files)
            throws Exception {
        configureCustom("invalid-additional-context");
        RecordingModel model = new RecordingModel();
        TestComponent component = component(model, new RecordingFilesystem(Map.of()));
        component.additionalContextFiles = files;

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("additional context"), failure.getMessage());
        assertEquals(0, model.calls.get());
    }

    static Stream<List<String>> invalidAdditionalContextFiles() {
        return Stream.of(
                java.util.Arrays.asList("context.md", null),
                List.of("context.md", " "),
                List.of("context.md", "context.md"));
    }

    @Test
    void nullCapabilityHooksPreserveHarnessDefaults() throws Exception {
        configureCustom("null-defaults");
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
        component.inspectBuilder = builder -> {
            assertFalse(booleanField(builder, "disableCompaction"));
            assertFalse(booleanField(builder, "disableToolResultEviction"));
            MemoryConfig upstreamDefault = (MemoryConfig) field(builder, "memoryConfig");
            assertEquals(MemoryConfig.DEFAULT_SESSION_RETENTION_DAYS,
                    upstreamDefault.sessionRetentionDays());
            assertEquals(MemoryConfig.DEFAULT_DAILY_FILE_RETENTION_DAYS,
                    upstreamDefault.dailyFileRetentionDays());
        };

        component.process();
    }

    @Test
    void explicitCompactionMemoryAndEvictionObjectsArePassedByIdentity() throws Exception {
        configureCustom("explicit-context-engineering");
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
        component.compaction = CompactionConfig.builder().triggerMessages(7).keepMessages(3).build();
        component.memory = MemoryConfig.builder().sessionRetentionDays(4).build();
        component.eviction = ToolResultEvictionConfig.builder()
                .maxResultChars(1234)
                .previewChars(12)
                .build();
        component.inspectBuilder = builder -> {
            assertSame(component.compaction, field(builder, "compactionConfig"));
            assertSame(component.memory, field(builder, "memoryConfig"));
            assertSame(component.eviction, field(builder, "toolResultEvictionConfig"));
        };

        component.process();
    }

    @Test
    void skillRepositoriesRemainAdditiveAndTheAllowlistReachesHarness() throws Exception {
        configureCustom("skills");
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
        RecordingRepository first = new RecordingRepository("first");
        RecordingRepository second = new RecordingRepository("second");
        component.repositories = List.of(first, second);
        component.skillFilter = SkillFilter.only("allowed-skill");
        component.inspectBuilder = builder -> {
            assertEquals(List.of(first, second), field(builder, "skillRepositories"));
            assertSame(component.skillFilter, field(builder, "skillFilter"));
        };

        component.process();

        assertEquals(List.of(first, second), component.runtime.agent().getSkillRepositories());
        assertTrue(component.skillFilter.isAllowed("allowed-skill"));
        assertFalse(component.skillFilter.isAllowed("blocked-skill"));
    }

    @Test
    void guardedLocalKeepsSubagentsDisabledWhileRetainingDeclarations() throws Exception {
        configureGuarded("guarded-subagents");
        TestComponent component = component(new RecordingModel(), null);
        SubagentDeclaration declaration = SubagentDeclaration.builder()
                .name("reviewer")
                .description("local reviewer")
                .inlineAgentsBody("Review carefully")
                .inheritParentPermissions(true)
                .build();
        component.subagents = List.of(declaration);
        component.inspectBuilder = builder -> {
            assertEquals(List.of(declaration), field(builder, "subagentDeclarations"));
            assertTrue(booleanField(builder, "disableSubagents"));
        };

        component.process();
    }

    @Test
    void taskPlanAndEvictionAreConfiguredWithoutAllowingPlanShell() throws Exception {
        configureCustom("task-plan");
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
        RecordingTaskRepository tasks = new RecordingTaskRepository();
        component.tasks = tasks;
        component.planMode = true;
        component.eviction = ToolResultEvictionConfig.builder().maxResultChars(2000).build();
        component.inspectBuilder = builder -> {
            assertSame(tasks, field(builder, "taskRepository"));
            assertTrue(booleanField(builder, "planModeEnabled"));
            assertFalse(booleanField(builder, "planModeAllowShell"));
            assertSame(component.eviction, field(builder, "toolResultEvictionConfig"));
        };

        component.process();
    }

    @Test
    void customizerCannotReplaceCapturedContextEngineeringObjects() throws Exception {
        configureCustom("customizer-replacement");
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
        component.compaction = CompactionConfig.builder().triggerMessages(5).build();
        component.memory = MemoryConfig.builder().sessionRetentionDays(5).build();
        component.eviction = ToolResultEvictionConfig.builder().maxResultChars(3000).build();
        component.additionalContextFiles = List.of("context.md");
        component.mutateBuilder = builder -> builder
                .compaction(CompactionConfig.builder().triggerMessages(99).build())
                .memory(MemoryConfig.defaults())
                .toolResultEviction(ToolResultEvictionConfig.defaults());

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("capabilit"), failure.getMessage());
    }

    private TestComponent component(RecordingModel model, RecordingFilesystem filesystem) {
        Slot slot = new Slot();
        slot.setChainId("capabilities-chain");
        slot.setConversationId("conversation");
        slot.putRequestId("request");
        TestComponent component = new TestComponent(slot, model, filesystem);
        component.setNodeId("capabilities-agent");
        components.add(component);
        return component;
    }

    private void configureCustom(String namespace) throws Exception {
        configure(namespace, HarnessFilesystemBackend.CUSTOM);
    }

    private void configureGuarded(String namespace) throws Exception {
        configure(namespace, HarnessFilesystemBackend.GUARDED_LOCAL);
    }

    private void configure(String namespace, HarnessFilesystemBackend backend) throws Exception {
        Path workspace = tempDir.resolve(namespace);
        Files.createDirectories(workspace);
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace(namespace);
        agent.getRuntime().setDefaultUserId("user");
        agent.getWorkspace().setRoot(workspace.toString());
        agent.getHarness().setFilesystemBackend(backend);
        agent.getHarness().setTrustedLocal(backend == HarnessFilesystemBackend.GUARDED_LOCAL);
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(config);
    }

    private static Object field(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            assertTrue(field.trySetAccessible(), name);
            return field.get(target);
        }
        catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static boolean booleanField(Object target, String name) {
        return (boolean) field(target, name);
    }

    private static final class TestComponent extends HarnessAgentComponent {
        private final Slot slot;
        private final RecordingModel model;
        private final RecordingFilesystem filesystem;
        private List<String> additionalContextFiles = List.of();
        private CompactionConfig compaction;
        private MemoryConfig memory;
        private ToolResultEvictionConfig eviction;
        private List<AgentSkillRepository> repositories = List.of();
        private SkillFilter skillFilter = SkillFilter.all();
        private List<SubagentDeclaration> subagents = List.of();
        private TaskRepository tasks;
        private boolean planMode;
        private Consumer<HarnessAgent.Builder> inspectBuilder = ignored -> { };
        private Consumer<HarnessAgent.Builder> mutateBuilder = ignored -> { };
        private HarnessAgentRuntime runtime;

        private TestComponent(Slot slot, RecordingModel model, RecordingFilesystem filesystem) {
            this.slot = slot;
            this.model = model;
            this.filesystem = filesystem;
        }

        @Override public Slot getSlot() { return slot; }
        @Override protected ModelSpec<?> model() { throw new AssertionError("buildModel used"); }
        @Override protected Model buildModel() { return model; }
        protected List<String> additionalContextFiles() { return additionalContextFiles; }
        @Override protected CompactionConfig compactionConfig() { return compaction; }
        @Override protected MemoryConfig memoryConfig() { return memory; }
        @Override protected ToolResultEvictionConfig toolResultEvictionConfig() { return eviction; }
        @Override protected List<AgentSkillRepository> skillRepositories() { return repositories; }
        @Override protected SkillFilter skillFilter() { return skillFilter; }
        @Override protected List<SubagentDeclaration> subagents() { return subagents; }
        @Override protected TaskRepository taskRepository() { return tasks; }
        @Override protected boolean enablePlanMode() { return planMode; }
        @Override protected PermissionContextState permissionContext() {
            return PermissionContextState.builder().build();
        }
        @Override protected HarnessFilesystemConfigurer filesystemConfigurer() {
            return filesystem == null ? null : (builder, context) -> builder.abstractFilesystem(filesystem);
        }
        @Override protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            inspectBuilder.accept(builder);
            mutateBuilder.accept(builder);
            return builder.disableDynamicSkills().disableDefaultWorkspaceSkills().disableToolsConfig()
                    .disableFilesystemTools().disableShellTool().disableMemoryTools().disableMemoryHooks()
                    .disableAtPathExpansion();
        }
        @Override protected void customizeRuntimeContext(
                RuntimeContext.Builder builder, LiteFlowAgentContext context) { }
        @Override protected String systemPrompt() { return "Answer deterministically."; }
        @Override protected String userPrompt(LiteFlowAgentContext context) { return "question"; }
        @Override protected HarnessAgentRuntime buildRuntime(AgentRuntimeBuildContext context) {
            runtime = super.buildRuntime(context);
            return runtime;
        }
    }

    private static final class RecordingModel implements Model {
        private final AtomicInteger calls = new AtomicInteger();
        private final List<List<Msg>> messages = new ArrayList<>();
        @Override public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            calls.incrementAndGet();
            this.messages.add(List.copyOf(messages));
            ContentBlock response = TextBlock.builder().text("done").build();
            return Flux.just(ChatResponse.builder().content(List.of(response)).finishReason("stop").build());
        }
        private String systemText() {
            return messages.stream().flatMap(Collection::stream)
                    .flatMap(msg -> msg.getContent().stream())
                    .filter(TextBlock.class::isInstance).map(TextBlock.class::cast)
                    .map(TextBlock::getText).reduce("", (left, right) -> left + "\n" + right);
        }
        @Override public String getModelName() { return "capabilities-model"; }
    }

    private static final class RecordingFilesystem implements AbstractFilesystem {
        private final Map<String, String> files;
        private RecordingFilesystem(Map<String, String> files) {
            this.files = new LinkedHashMap<>(files);
        }
        @Override public LsResult ls(RuntimeContext context, String path) { throw unused(); }
        @Override public ReadResult read(RuntimeContext context, String path, int offset, int limit) {
            String value = files.get(path);
            return value == null ? ReadResult.fail("not found") : ReadResult.success(FileData.create(value));
        }
        @Override public WriteResult write(RuntimeContext context, String path, String content) { return WriteResult.ok(path); }
        @Override public EditResult edit(RuntimeContext context, String path, String oldText, String newText, boolean all) { throw unused(); }
        @Override public GrepResult grep(RuntimeContext context, String pattern, String path, String glob) { throw unused(); }
        @Override public GlobResult glob(RuntimeContext context, String pattern, String path) {
            return GlobResult.success(List.of());
        }
        @Override public List<FileUploadResponse> uploadFiles(RuntimeContext context, List<Map.Entry<String, byte[]>> files) { throw unused(); }
        @Override public List<FileDownloadResponse> downloadFiles(RuntimeContext context, List<String> paths) { throw unused(); }
        @Override public WriteResult delete(RuntimeContext context, String path) { throw unused(); }
        @Override public WriteResult move(RuntimeContext context, String from, String to) { throw unused(); }
        @Override public boolean exists(RuntimeContext context, String path) { return files.containsKey(path); }
        private UnsupportedOperationException unused() { return new UnsupportedOperationException(); }
    }

    private static final class RecordingRepository implements AgentSkillRepository {
        private final String name;
        private RecordingRepository(String name) { this.name = name; }
        @Override public AgentSkill getSkill(String skillName) { return null; }
        @Override public List<String> getAllSkillNames() { return List.of(); }
        @Override public List<AgentSkill> getAllSkills() { return List.of(); }
        @Override public boolean save(List<AgentSkill> skills, boolean force) { return false; }
        @Override public boolean delete(String skillName) { return false; }
        @Override public boolean skillExists(String skillName) { return false; }
        @Override public AgentSkillRepositoryInfo getRepositoryInfo() { return null; }
        @Override public String getSource() { return name; }
        @Override public void setWriteable(boolean writeable) { }
        @Override public boolean isWriteable() { return false; }
    }

    private static final class RecordingTaskRepository implements TaskRepository {
        @Override public BackgroundTask getTask(RuntimeContext context, String agentId, String taskId) { return null; }
        @Override public BackgroundTask putTask(RuntimeContext context, String agentId, String taskId, String description, TaskRunSpec spec) { return null; }
        @Override public void removeTask(RuntimeContext context, String agentId, String taskId) { }
        @Override public void clear() { }
        @Override public Collection<BackgroundTask> listTasks(RuntimeContext context, String agentId, TaskStatus status) { return List.of(); }
        @Override public boolean cancelTask(RuntimeContext context, String agentId, String taskId) { return false; }
    }
}
