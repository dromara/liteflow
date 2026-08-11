package com.yomahub.liteflow.agent.harness.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.AgentBase;
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
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.model.EditResult;
import io.agentscope.harness.agent.filesystem.model.FileDownloadResponse;
import io.agentscope.harness.agent.filesystem.model.FileUploadResponse;
import io.agentscope.harness.agent.filesystem.model.GlobResult;
import io.agentscope.harness.agent.filesystem.model.GrepResult;
import io.agentscope.harness.agent.filesystem.model.LsResult;
import io.agentscope.harness.agent.filesystem.model.ReadResult;
import io.agentscope.harness.agent.filesystem.model.WriteResult;
import io.agentscope.harness.agent.subagent.task.BackgroundTask;
import io.agentscope.harness.agent.subagent.task.TaskRepository;
import io.agentscope.harness.agent.subagent.task.TaskRunSpec;
import io.agentscope.harness.agent.subagent.task.TaskStatus;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessAgentComponentTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String AGENT_NAMESPACE =
            "lf-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @TempDir
    Path tempDir;

    private final List<TestComponent> components = new ArrayList<>();

    @AfterEach
    void clearConfigAndCloseComponents() {
        components.forEach(TestComponent::close);
        LiteflowConfigGetter.clean();
    }

    @Test
    void componentUsesCoreFinalTemplateAndBuildsOneRuntimeForRepeatedCalls() throws Exception {
        configureCustomBackend();
        RecordingFilesystem filesystem = new RecordingFilesystem("filesystem", new ArrayList<>());
        RecordingModel model = new RecordingModel("plain reply", false, null, null, null);
        TestComponent component = component(slot("conversation-1", "request-1"), model, filesystem);

        component.process();
        component.process();

        assertEquals("plain reply", component.getSlot().getResponseData());
        assertEquals(1, component.modelBuildCount.get());
        assertEquals(1, component.customizeCount.get());
        assertEquals(2, model.callCount.get());
        assertEquals(
                com.yomahub.liteflow.agent.component.AbstractAgentComponent.class,
                HarnessAgentComponent.class.getMethod("process").getDeclaringClass());
        assertSame(filesystem, component.runtime().agent().getWorkspaceManager().getFilesystem());
    }

    @Test
    void differentSessionsExecuteConcurrentlyOnOneHarnessRuntime() throws Exception {
        configureCustomBackend();
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        RecordingModel model = new RecordingModel("parallel reply", false, entered, release, null);
        TestComponent component = component(
                slot("unused", "bootstrap"),
                model,
                new RecordingFilesystem("filesystem", new ArrayList<>()));
        Slot firstSlot = slot("conversation-a", "request-a");
        Slot secondSlot = slot("conversation-b", "request-b");
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> component.process(firstSlot));
            Future<?> second = executor.submit(() -> component.process(secondSlot));

            assertTrue(entered.await(5, TimeUnit.SECONDS));
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        }
        finally {
            release.countDown();
            executor.shutdownNow();
        }

        assertEquals("parallel reply", firstSlot.getResponseData());
        assertEquals("parallel reply", secondSlot.getResponseData());
        assertEquals(1, component.modelBuildCount.get());
        assertEquals(2, model.callCount.get());
        assertEquals(2, model.runtimeContexts.stream()
                .map(RuntimeContext::getSessionId)
                .distinct()
                .count());
    }

    @Test
    void textJavaTypeAndJsonSchemaOutputsUseTheSharedExecutor() throws Exception {
        configureCustomBackend();

        TestComponent text = component(
                slot("text-session", "text-request"),
                new RecordingModel("text reply", false, null, null, null),
                new RecordingFilesystem("text-filesystem", new ArrayList<>()));
        text.process();
        assertEquals("text reply", text.getSlot().getResponseData());

        TestComponent typed = component(
                slot("typed-session", "typed-request"),
                new RecordingModel("{\"answer\":\"typed reply\"}", true, null, null, null),
                new RecordingFilesystem("typed-filesystem", new ArrayList<>()));
        typed.outputType = StructuredReply.class;
        typed.process();
        StructuredReply typedReply = typed.getSlot().getResponseData();
        assertEquals("typed reply", typedReply.answer);

        TestComponent schema = component(
                slot("schema-session", "schema-request"),
                new RecordingModel("{\"answer\":\"schema reply\"}", true, null, null, null),
                new RecordingFilesystem("schema-filesystem", new ArrayList<>()));
        schema.outputSchema = schema();
        schema.process();
        JsonNode schemaReply = schema.getSlot().getResponseData();
        assertEquals("schema reply", schemaReply.path("answer").asText());
    }

    @Test
    void unimplementedBackendsAndMissingCustomConfigurerFailClosedBeforeModelBuild()
            throws Exception {
        AgentConfig config = configureAgent();
        TestComponent component = component(
                slot("backend-session", "backend-request"),
                new RecordingModel("must not run", false, null, null, null),
                null);

        AgentConfigException untrusted = assertThrows(AgentConfigException.class, component::process);
        assertTrue(untrusted.getMessage().contains("trusted-local"));

        config.getHarness().setTrustedLocal(true);
        AgentConfigException guarded = assertThrows(AgentConfigException.class, component::process);
        assertTrue(guarded.getMessage().contains("GUARDED_LOCAL"));

        config.getHarness().setFilesystemBackend(HarnessFilesystemBackend.DOCKER);
        AgentConfigException docker = assertThrows(AgentConfigException.class, component::process);
        assertTrue(docker.getMessage().contains("DOCKER"));

        config.getHarness().setFilesystemBackend(HarnessFilesystemBackend.CUSTOM);
        AgentConfigException custom = assertThrows(AgentConfigException.class, component::process);
        assertTrue(custom.getMessage().contains("filesystemConfigurer"));
        assertEquals(0, component.modelBuildCount.get());
    }

    @Test
    void customConfigurerCannotFallBackToHarnessHostLocalFilesystem() throws Exception {
        configureCustomBackend();
        RecordingModel model = new RecordingModel("must not run", false, null, null, null);
        TestComponent component = component(
                slot("no-op-session", "no-op-request"), model, null);
        component.explicitFilesystemConfigurer = (builder, context) -> { };

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("filesystemConfigurer"));
        assertEquals(1, model.closeCount.get());
    }

    @Test
    void closeIsIdempotentUsesReverseOwnershipOrderAndLeavesBorrowedResourcesOpen()
            throws Exception {
        configureCustomBackend();
        List<String> order = new CopyOnWriteArrayList<>();
        RecordingStore ownedStore = new RecordingStore("store", order, null);
        RecordingModel model = new RecordingModel("reply", false, null, null, order);
        RecordingFilesystem ownedFilesystem = new RecordingFilesystem("filesystem", order);
        RecordingMcpClient ownedMcp = new RecordingMcpClient("mcp", order);
        RecordingMcpClient borrowedMcp = new RecordingMcpClient("borrowed-mcp", order);
        RecordingRepository ownedRepository = new RecordingRepository("repository", order, null);
        RecordingRepository borrowedRepository =
                new RecordingRepository("borrowed-repository", order, null);
        RecordingTaskRepository ownedTasks = new RecordingTaskRepository("tasks", order);
        TestComponent component = component(
                slot("ownership-session", "ownership-request"), model, ownedFilesystem);
        component.stateStoreResolver = ignored -> new ResolvedAgentStateStore(ownedStore, true);
        component.mcpClients = List.of(ownedMcp, borrowedMcp);
        component.ownedMcp = ownedMcp;
        component.repositories = List.of(ownedRepository, borrowedRepository);
        component.ownedRepository = ownedRepository;
        component.taskRepository = ownedTasks;
        component.ownsTaskRepository = true;
        component.ownedHarnessResources = List.of(ownedFilesystem);

        component.process();
        component.close();
        component.close();

        assertEquals(List.of(
                "tasks", "filesystem", "mcp", "repository", "model", "store"), order);
        assertEquals(0, borrowedMcp.closeCount.get());
        assertEquals(0, borrowedRepository.closeCount.get());

        List<String> borrowedOrder = new CopyOnWriteArrayList<>();
        RecordingFilesystem borrowedFilesystem =
                new RecordingFilesystem("borrowed-filesystem", borrowedOrder);
        RecordingTaskRepository borrowedTasks =
                new RecordingTaskRepository("borrowed-tasks", borrowedOrder);
        RecordingStore borrowedStore = new RecordingStore("borrowed-store", borrowedOrder, null);
        TestComponent borrowed = component(
                slot("borrowed-session", "borrowed-request"),
                new RecordingModel("reply", false, null, null, borrowedOrder),
                borrowedFilesystem);
        borrowed.stateStoreResolver = ignored -> new ResolvedAgentStateStore(borrowedStore, false);
        borrowed.taskRepository = borrowedTasks;
        borrowed.process();
        borrowed.close();

        assertEquals(0, borrowedFilesystem.closeCount.get());
        assertEquals(0, borrowedTasks.closeCount.get());
        assertFalse(borrowedOrder.contains("borrowed-store"));
    }

    @Test
    void buildRollbackKeepsPrimaryFailureAndSuppressesEveryCloseFailure() throws Exception {
        configureCustomBackend();
        List<String> order = new CopyOnWriteArrayList<>();
        RuntimeException storeClose = new RuntimeException("store close failed");
        RuntimeException repositoryClose = new RuntimeException("repository close failed");
        RuntimeException primary = new RuntimeException("customize failed");
        RecordingStore store = new RecordingStore("store", order, storeClose);
        RecordingModel model = new RecordingModel("unused", false, null, null, order);
        model.closeFailure = new RuntimeException("model close failed");
        RecordingRepository repository =
                new RecordingRepository("repository", order, repositoryClose);
        RecordingFilesystem filesystem = new RecordingFilesystem("filesystem", order);
        filesystem.closeFailure = new RuntimeException("filesystem close failed");
        TestComponent component = component(
                slot("rollback-session", "rollback-request"), model, filesystem);
        component.stateStoreResolver = ignored -> new ResolvedAgentStateStore(store, true);
        component.repositories = List.of(repository);
        component.ownedRepository = repository;
        component.ownedHarnessResources = List.of(filesystem);
        component.customizeFailure = primary;

        RuntimeException thrown = assertThrows(RuntimeException.class, component::process);

        assertSame(primary, thrown);
        assertEquals(List.of("filesystem", "repository", "model", "store"), order);
        assertEquals(4, thrown.getSuppressed().length);
    }

    @Test
    void customizerCannotReplaceLiteFlowNamespacedStateStore() throws Exception {
        configureCustomBackend();
        RecordingModel model = new RecordingModel("unused", false, null, null, new ArrayList<>());
        TestComponent component = component(
                slot("customizer-session", "customizer-request"),
                model,
                new RecordingFilesystem("filesystem", new ArrayList<>()));
        component.replaceStateStore = true;

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("namespaced StateStore"));
        assertEquals(1, model.closeCount.get());
    }

    @Test
    void customizerCannotReplaceThePreparedHarnessBuilder() throws Exception {
        configureCustomBackend();
        RecordingModel model = new RecordingModel("unused", false, null, null, new ArrayList<>());
        TestComponent component = component(
                slot("replacement-session", "replacement-request"),
                model,
                new RecordingFilesystem("filesystem", new ArrayList<>()));
        component.replaceBuilder = true;

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("provided builder"));
        assertEquals(1, model.closeCount.get());
    }

    private TestComponent component(Slot slot, RecordingModel model, RecordingFilesystem filesystem) {
        TestComponent component = new TestComponent(slot, model, filesystem);
        component.setNodeId("harness-agent");
        components.add(component);
        return component;
    }

    private void configureCustomBackend() throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setFilesystemBackend(HarnessFilesystemBackend.CUSTOM);
    }

    private AgentConfig configureAgent() throws Exception {
        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace("harness-test");
        agent.getRuntime().setDefaultUserId("test-user");
        agent.getRuntime().setTimeout(Duration.ofSeconds(5));
        agent.getWorkspace().setRoot(workspace.toString());
        LiteflowConfig liteflow = new LiteflowConfig();
        liteflow.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(liteflow);
        return agent;
    }

    private static Slot slot(String conversationId, String requestId) {
        Slot slot = new Slot();
        slot.setChainId("harness-chain");
        slot.setConversationId(conversationId);
        slot.putRequestId(requestId);
        return slot;
    }

    private static JsonNode schema() {
        return MAPPER.createObjectNode()
                .put("type", "object")
                .set("properties", MAPPER.createObjectNode()
                        .set("answer", MAPPER.createObjectNode().put("type", "string")));
    }

    public static final class StructuredReply {
        public String answer;
    }

    private static final class TestComponent extends HarnessAgentComponent {
        private final Slot defaultSlot;
        private final ThreadLocal<Slot> invocationSlot = new ThreadLocal<>();
        private final RecordingModel model;
        private final RecordingFilesystem filesystem;
        private final AtomicInteger modelBuildCount = new AtomicInteger();
        private final AtomicInteger customizeCount = new AtomicInteger();
        private AgentStateStoreResolver stateStoreResolver;
        private List<McpClientWrapper> mcpClients = List.of();
        private McpClientWrapper ownedMcp;
        private List<AgentSkillRepository> repositories = List.of();
        private AgentSkillRepository ownedRepository;
        private TaskRepository taskRepository;
        private boolean ownsTaskRepository;
        private List<? extends AutoCloseable> ownedHarnessResources = List.of();
        private Class<?> outputType;
        private JsonNode outputSchema;
        private RuntimeException customizeFailure;
        private boolean replaceStateStore;
        private boolean replaceBuilder;
        private HarnessFilesystemConfigurer explicitFilesystemConfigurer;
        private HarnessAgentRuntime runtime;

        private TestComponent(
                Slot defaultSlot, RecordingModel model, RecordingFilesystem filesystem) {
            this.defaultSlot = defaultSlot;
            this.model = model;
            this.filesystem = filesystem;
        }

        private void process(Slot slot) {
            invocationSlot.set(slot);
            try {
                try {
                    process();
                }
                catch (Exception failure) {
                    throw new RuntimeException(failure);
                }
            }
            finally {
                invocationSlot.remove();
            }
        }

        private HarnessAgentRuntime runtime() {
            return runtime;
        }

        @Override
        public Slot getSlot() {
            Slot current = invocationSlot.get();
            return current == null ? defaultSlot : current;
        }

        @Override
        protected ModelSpec<?> model() {
            throw new AssertionError("buildModel override must be used");
        }

        @Override
        protected Model buildModel() {
            modelBuildCount.incrementAndGet();
            return model;
        }

        @Override
        protected AgentStateStoreResolver stateStoreResolver() {
            return stateStoreResolver == null ? super.stateStoreResolver() : stateStoreResolver;
        }

        @Override
        protected List<McpClientWrapper> mcpClients() {
            return mcpClients;
        }

        @Override
        protected boolean ownsMcpClient(McpClientWrapper client) {
            return client == ownedMcp;
        }

        @Override
        protected List<AgentSkillRepository> skillRepositories() {
            return repositories;
        }

        @Override
        protected boolean ownsSkillRepository(AgentSkillRepository repository) {
            return repository == ownedRepository;
        }

        @Override
        protected TaskRepository taskRepository() {
            return taskRepository;
        }

        @Override
        protected boolean ownsTaskRepository(TaskRepository repository) {
            return ownsTaskRepository && repository == taskRepository;
        }

        @Override
        protected List<? extends AutoCloseable> ownedHarnessResources() {
            return ownedHarnessResources;
        }

        @Override
        protected HarnessFilesystemConfigurer filesystemConfigurer() {
            if (explicitFilesystemConfigurer != null) {
                return explicitFilesystemConfigurer;
            }
            if (filesystem == null) {
                return null;
            }
            return (builder, context) -> builder.abstractFilesystem(filesystem);
        }

        @Override
        protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            customizeCount.incrementAndGet();
            if (customizeFailure != null) {
                throw customizeFailure;
            }
            if (replaceStateStore) {
                builder.stateStore(new InMemoryAgentStateStore());
            }
            if (replaceBuilder) {
                return HarnessAgent.builder();
            }
            return builder
                    .disableCompaction()
                    .disableToolResultEviction()
                    .disableMemoryTools()
                    .disableMemoryHooks()
                    .disableWorkspaceContext()
                    .disableAtPathExpansion()
                    .disableSubagents()
                    .disableDefaultWorkspaceSkills()
                    .disableDynamicSkills()
                    .disableToolsConfig()
                    .disableFilesystemTools()
                    .disableShellTool();
        }

        @Override
        protected PermissionContextState permissionContext() {
            return PermissionContextState.builder().build();
        }

        @Override
        protected Class<?> structuredOutputType() {
            return outputType;
        }

        @Override
        protected JsonNode structuredOutputSchema() {
            return outputSchema;
        }

        @Override
        protected String systemPrompt() {
            return "Answer deterministically.";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "question";
        }

        @Override
        protected HarnessAgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
            runtime = super.buildRuntime(buildContext);
            return runtime;
        }
    }

    private static final class RecordingModel implements Model, AutoCloseable {
        private final String response;
        private final boolean nativeStructured;
        private final CountDownLatch entered;
        private final CountDownLatch release;
        private final List<String> closeOrder;
        private final AtomicInteger callCount = new AtomicInteger();
        private final AtomicInteger closeCount = new AtomicInteger();
        private final List<RuntimeContext> runtimeContexts = new CopyOnWriteArrayList<>();
        private RuntimeException closeFailure;

        private RecordingModel(
                String response,
                boolean nativeStructured,
                CountDownLatch entered,
                CountDownLatch release,
                List<String> closeOrder) {
            this.response = response;
            this.nativeStructured = nativeStructured;
            this.entered = entered;
            this.release = release;
            this.closeOrder = closeOrder;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            callCount.incrementAndGet();
            return Flux.deferContextual(context -> {
                runtimeContexts.add(context.get(AgentBase.RUNTIME_CONTEXT_KEY));
                if (entered != null) {
                    entered.countDown();
                }
                if (release != null) {
                    try {
                        if (!release.await(5, TimeUnit.SECONDS)) {
                            return Flux.error(new IllegalStateException("release timed out"));
                        }
                    }
                    catch (InterruptedException failure) {
                        Thread.currentThread().interrupt();
                        return Flux.error(failure);
                    }
                }
                ContentBlock content = TextBlock.builder().text(response).build();
                return Flux.just(ChatResponse.builder()
                        .content(List.of(content))
                        .finishReason("stop")
                        .build());
            });
        }

        @Override
        public boolean supportsNativeStructuredOutput() {
            return nativeStructured;
        }

        @Override
        public String getModelName() {
            return "harness-test-model";
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            if (closeOrder != null) {
                closeOrder.add("model");
            }
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }

    private static final class RecordingFilesystem implements AbstractFilesystem, AutoCloseable {
        private final String name;
        private final List<String> closeOrder;
        private final AtomicInteger closeCount = new AtomicInteger();
        private RuntimeException closeFailure;

        private RecordingFilesystem(String name, List<String> closeOrder) {
            this.name = name;
            this.closeOrder = closeOrder;
        }

        @Override public LsResult ls(RuntimeContext context, String path) { throw unused(); }
        @Override public ReadResult read(RuntimeContext context, String path, int offset, int limit) { throw unused(); }
        @Override public WriteResult write(RuntimeContext context, String path, String content) { throw unused(); }
        @Override public EditResult edit(RuntimeContext context, String path, String oldText, String newText, boolean all) { throw unused(); }
        @Override public GrepResult grep(RuntimeContext context, String pattern, String path, String glob) { throw unused(); }
        @Override public GlobResult glob(RuntimeContext context, String pattern, String path) { throw unused(); }
        @Override public List<FileUploadResponse> uploadFiles(RuntimeContext context, List<Map.Entry<String, byte[]>> files) { throw unused(); }
        @Override public List<FileDownloadResponse> downloadFiles(RuntimeContext context, List<String> paths) { throw unused(); }
        @Override public WriteResult delete(RuntimeContext context, String path) { throw unused(); }
        @Override public WriteResult move(RuntimeContext context, String from, String to) { throw unused(); }
        @Override public boolean exists(RuntimeContext context, String path) { return false; }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            closeOrder.add(name);
            if (closeFailure != null) {
                throw closeFailure;
            }
        }

        private static UnsupportedOperationException unused() {
            return new UnsupportedOperationException("filesystem operation is not used");
        }
    }

    private static final class RecordingStore extends InMemoryAgentStateStore {
        private final String name;
        private final List<String> closeOrder;
        private final RuntimeException closeFailure;

        private RecordingStore(String name, List<String> closeOrder, RuntimeException closeFailure) {
            this.name = name;
            this.closeOrder = closeOrder;
            this.closeFailure = closeFailure;
        }

        @Override
        public void close() {
            closeOrder.add(name);
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }

    private static final class RecordingMcpClient extends McpClientWrapper {
        private final String name;
        private final List<String> closeOrder;
        private final AtomicInteger closeCount = new AtomicInteger();

        private RecordingMcpClient(String name, List<String> closeOrder) {
            super(name);
            this.name = name;
            this.closeOrder = closeOrder;
        }

        @Override public Mono<Void> initialize() { return Mono.empty(); }
        @Override public Mono<List<McpSchema.Tool>> listTools() { return Mono.just(List.of()); }
        @Override public Mono<McpSchema.CallToolResult> callTool(String name, Map<String, Object> args) { return Mono.error(unused()); }
        @Override public Mono<McpSchema.CallToolResult> callTool(String name, Map<String, Object> args, Map<String, Object> meta) { return Mono.error(unused()); }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            closeOrder.add(name);
        }
    }

    private static final class RecordingRepository implements AgentSkillRepository {
        private final String name;
        private final List<String> closeOrder;
        private final RuntimeException closeFailure;
        private final AtomicInteger closeCount = new AtomicInteger();

        private RecordingRepository(String name, List<String> closeOrder, RuntimeException closeFailure) {
            this.name = name;
            this.closeOrder = closeOrder;
            this.closeFailure = closeFailure;
        }

        @Override public AgentSkill getSkill(String name) { return null; }
        @Override public List<String> getAllSkillNames() { return List.of(); }
        @Override public List<AgentSkill> getAllSkills() { return List.of(); }
        @Override public boolean save(List<AgentSkill> skills, boolean force) { return false; }
        @Override public boolean delete(String skillName) { return false; }
        @Override public boolean skillExists(String skillName) { return false; }
        @Override public AgentSkillRepositoryInfo getRepositoryInfo() { return new AgentSkillRepositoryInfo("test", name, false); }
        @Override public String getSource() { return name; }
        @Override public void setWriteable(boolean writeable) { }
        @Override public boolean isWriteable() { return false; }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            closeOrder.add(name);
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }

    private static final class RecordingTaskRepository implements TaskRepository, AutoCloseable {
        private final String name;
        private final List<String> closeOrder;
        private final AtomicInteger closeCount = new AtomicInteger();

        private RecordingTaskRepository(String name, List<String> closeOrder) {
            this.name = name;
            this.closeOrder = closeOrder;
        }

        @Override public BackgroundTask getTask(RuntimeContext context, String agentId, String taskId) { return null; }
        @Override public BackgroundTask putTask(RuntimeContext context, String agentId, String taskId, String description, TaskRunSpec spec) { return null; }
        @Override public void removeTask(RuntimeContext context, String agentId, String taskId) { }
        @Override public void clear() { }
        @Override public Collection<BackgroundTask> listTasks(RuntimeContext context, String agentId, TaskStatus status) { return List.of(); }
        @Override public boolean cancelTask(RuntimeContext context, String agentId, String taskId) { return false; }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            closeOrder.add(name);
        }
    }

    private static UnsupportedOperationException unused() {
        return new UnsupportedOperationException("not used");
    }
}
