package com.yomahub.liteflow.agent.runtime;

import com.yomahub.liteflow.agent.component.AbstractAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class AgentComponentLifecycleTest {

    private static final String AGENT_NAMESPACE =
            "lf-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @AfterEach
    void clearGlobalConfig() {
        LiteflowConfigGetter.clean();
    }

    @Test
    void componentPublishesTheCloseableLifecycleContractUsedByContainers() throws Exception {
        assertTrue(Closeable.class.isAssignableFrom(AbstractAgentComponent.class));
        assertTrue(Modifier.isFinal(AbstractAgentComponent.class.getMethod("process").getModifiers()));
        assertTrue(Modifier.isFinal(AbstractAgentComponent.class.getMethod("close").getModifiers()));
    }

    @Test
    void concurrentInvocationsShareOneRuntimeAndCloseWaitsThenClosesOnce() throws Exception {
        configureAgent();
        TestComponent component = new TestComponent();
        CountDownLatch invocationsEntered = new CountDownLatch(2);
        CountDownLatch releaseInvocations = new CountDownLatch(1);
        component.reply = Mono.fromCallable(() -> {
            invocationsEntered.countDown();
            assertTrue(releaseInvocations.await(5, TimeUnit.SECONDS));
            return AssistantMessage.builder().textContent("done").build();
        });
        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            Future<?> first = executor.submit(() -> {
                component.process();
                return null;
            });
            Future<?> second = executor.submit(() -> {
                component.process();
                return null;
            });
            assertTrue(invocationsEntered.await(5, TimeUnit.SECONDS));

            Future<?> close = executor.submit(() -> {
                component.close();
                return null;
            });
            assertFalse(close.isDone());
            assertEquals(0, component.runtime.get().closeCount.get());

            releaseInvocations.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            close.get(5, TimeUnit.SECONDS);

            assertEquals(1, component.buildCount.get());
            assertEquals(1, component.runtime.get().closeCount.get());
            component.close();
            assertEquals(1, component.runtime.get().closeCount.get());
            assertThrows(IllegalStateException.class, component::process);
        } finally {
            releaseInvocations.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void runtimeClosesOnlyOwnedResourcesInTheDocumentedOrder() {
        List<String> order = new ArrayList<>();
        ReActAgent agent = mock(ReActAgent.class);
        doAnswer(invocation -> {
            order.add("agent");
            return null;
        }).when(agent).close();
        RecordingStore ownedStore = new RecordingStore("store", order);
        RecordingStore borrowedStore = new RecordingStore("borrowed-store", order);
        RecordingMcpClient firstMcp = new RecordingMcpClient("mcp-1", order);
        RecordingMcpClient secondMcp = new RecordingMcpClient("mcp-2", order);
        RecordingMcpClient borrowedMcp = new RecordingMcpClient("borrowed-mcp", order);
        RecordingRepository firstRepository = new RecordingRepository("repo-1", order);
        RecordingRepository secondRepository = new RecordingRepository("repo-2", order);
        RecordingModel firstModel = new RecordingModel("model-1", order);
        RecordingModel secondModel = new RecordingModel("model-2", order);
        GuardedNamespacedAgentStateStore namespaced =
                new GuardedNamespacedAgentStateStore(ownedStore, AGENT_NAMESPACE);
        ReActAgentRuntime runtime = new ReActAgentRuntime(
                agent,
                namespaced,
                new ResolvedAgentStateStore(ownedStore, true),
                List.of(
                        new McpClientRegistration(firstMcp, true),
                        new McpClientRegistration(borrowedMcp, false),
                        new McpClientRegistration(secondMcp, true)),
                List.of(firstRepository, secondRepository),
                List.of(firstModel, secondModel));

        runtime.close();
        runtime.close();
        new ResolvedAgentStateStore(borrowedStore, false).close();

        assertEquals(List.of(
                "agent", "mcp-2", "mcp-1", "repo-2", "repo-1",
                "model-2", "model-1", "store"), order);
        assertEquals(0, borrowedMcp.closeCount.get());
        assertEquals(0, borrowedStore.closeCount.get());
    }

    private static void configureAgent() {
        AgentConfig agent = new AgentConfig();
        agent.getStateStore().setJsonRoot("target/agent-state");
        agent.getRuntime().setNamespace("lifecycle-test");
        agent.getRuntime().setTimeout(Duration.ofSeconds(10));
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(config);
    }

    private static final class TestComponent extends AbstractAgentComponent<TestRuntime> {
        private final Slot slot = new Slot();
        private final AtomicInteger buildCount = new AtomicInteger();
        private final AtomicInteger conversationSequence = new AtomicInteger();
        private final AtomicReference<TestRuntime> runtime = new AtomicReference<>();
        private Mono<Msg> reply;

        private TestComponent() {
            slot.setChainId("lifecycle-chain");
            slot.putRequestId("lifecycle-request");
            setNodeId("lifecycle-agent");
        }

        @Override
        public Slot getSlot() {
            return slot;
        }

        @Override
        protected String resolveConversationId(Slot slot) {
            return "lifecycle-conversation-" + conversationSequence.incrementAndGet();
        }

        @Override
        protected TestRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
            buildCount.incrementAndGet();
            TestRuntime built = new TestRuntime();
            runtime.set(built);
            return built;
        }

        @Override
        protected Mono<Msg> invokeRuntime(
                TestRuntime runtime,
                List<Msg> input,
                AgentOutputSpec output,
                RuntimeContext runtimeContext,
                LiteFlowAgentContext liteflowContext) {
            return reply;
        }

        @Override
        protected String systemPrompt() {
            return "lifecycle";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "question";
        }
    }

    private static final class TestRuntime implements AutoCloseable {
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    private static final class RecordingMcpClient extends McpClientWrapper {
        private final String resourceName;
        private final List<String> order;
        private final AtomicInteger closeCount = new AtomicInteger();

        private RecordingMcpClient(String resourceName, List<String> order) {
            super(resourceName);
            this.resourceName = resourceName;
            this.order = order;
        }

        @Override
        public Mono<Void> initialize() {
            return Mono.empty();
        }

        @Override
        public Mono<List<McpSchema.Tool>> listTools() {
            return Mono.just(List.of());
        }

        @Override
        public Mono<McpSchema.CallToolResult> callTool(
                String toolName, Map<String, Object> arguments) {
            return Mono.error(new UnsupportedOperationException("not used"));
        }

        @Override
        public Mono<McpSchema.CallToolResult> callTool(
                String toolName, Map<String, Object> arguments, Map<String, Object> meta) {
            return Mono.error(new UnsupportedOperationException("not used"));
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            order.add(resourceName);
        }
    }

    private static final class RecordingRepository implements AgentSkillRepository {
        private final String name;
        private final List<String> order;

        private RecordingRepository(String name, List<String> order) {
            this.name = name;
            this.order = order;
        }

        @Override public AgentSkill getSkill(String name) { return null; }
        @Override public List<String> getAllSkillNames() { return List.of(); }
        @Override public List<AgentSkill> getAllSkills() { return List.of(); }
        @Override public boolean save(List<AgentSkill> skills, boolean force) { return false; }
        @Override public boolean delete(String skillName) { return false; }
        @Override public boolean skillExists(String skillName) { return false; }
        @Override public AgentSkillRepositoryInfo getRepositoryInfo() {
            return new AgentSkillRepositoryInfo("test", name, false);
        }
        @Override public String getSource() { return name; }
        @Override public void setWriteable(boolean writeable) { }
        @Override public boolean isWriteable() { return false; }

        @Override
        public void close() {
            order.add(name);
        }
    }

    private static final class RecordingModel implements Model, AutoCloseable {
        private final String name;
        private final List<String> order;

        private RecordingModel(String name, List<String> order) {
            this.name = name;
            this.order = order;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            return Flux.error(new UnsupportedOperationException("not used"));
        }

        @Override
        public String getModelName() {
            return name;
        }

        @Override
        public void close() {
            order.add(name);
        }
    }

    private static final class RecordingStore implements AgentStateStore {
        private final String name;
        private final List<String> order;
        private final AtomicInteger closeCount = new AtomicInteger();

        private RecordingStore(String name, List<String> order) {
            this.name = name;
            this.order = order;
        }

        @Override public void save(String userId, String sessionId, String key, State state) { }
        @Override public void save(
                String userId, String sessionId, String key, List<? extends State> states) { }
        @Override public <T extends State> Optional<T> get(
                String userId, String sessionId, String key, Class<T> type) {
            return Optional.empty();
        }
        @Override public <T extends State> List<T> getList(
                String userId, String sessionId, String key, Class<T> type) {
            return List.of();
        }
        @Override public boolean exists(String userId, String sessionId) { return false; }
        @Override public void delete(String userId, String sessionId) { }
        @Override public Set<String> listSessionIds(String userId) { return Set.of(); }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            order.add(name);
        }
    }
}
