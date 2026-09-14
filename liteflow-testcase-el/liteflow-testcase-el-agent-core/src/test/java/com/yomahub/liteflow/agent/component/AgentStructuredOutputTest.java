package com.yomahub.liteflow.agent.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.agent.testsupport.ScriptedChatModel;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreFailurePolicy;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentStructuredOutputTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LiteflowConfig previousConfig;
    private final List<TestComponent> components = new ArrayList<>();

    @AfterEach
    void restoreGlobalConfigAndCloseComponents() {
        components.forEach(TestComponent::close);
        if (previousConfig != null) {
            LiteflowConfigGetter.setLiteflowConfig(previousConfig);
        } else {
            LiteflowConfigGetter.clean();
        }
    }

    @Test
    void realAgentCallsWriteTextJavaTypeAndJsonSchemaReplies() throws Exception {
        configureAgent();

        Slot textSlot = slot("text-request");
        ScriptedChatModel textModel = new ScriptedChatModel("plain reply");
        TestComponent text = component(textSlot, textModel);
        text.process();
        assertEquals("plain reply", textSlot.getResponseData());

        Slot typedSlot = slot("typed-request");
        ScriptedChatModel typedModel = ScriptedChatModel.nativeStructuredJson(
                "{\"answer\":\"typed reply\"}");
        TestComponent typed = component(typedSlot, typedModel);
        typed.outputType = StructuredReply.class;
        typed.process();
        StructuredReply typedReply = typedSlot.getResponseData();
        assertEquals("typed reply", typedReply.answer);

        Slot schemaSlot = slot("schema-request");
        ScriptedChatModel schemaModel = ScriptedChatModel.nativeStructuredJson(
                "{\"answer\":\"schema reply\"}");
        TestComponent schema = component(schemaSlot, schemaModel);
        schema.outputSchema = schema();
        schema.process();
        JsonNode schemaReply = schemaSlot.getResponseData();
        assertEquals("schema reply", schemaReply.path("answer").asText());
    }

    @Test
    void dynamicSystemPromptIsAwaitedExactlyOncePerCallAndReachesModel() throws Exception {
        configureAgent();
        Slot slot = slot("prompt-request");
        ScriptedChatModel model = new ScriptedChatModel("reply");
        TestComponent component = component(slot, model);

        component.process();
        component.process();

        assertEquals(1, component.systemPromptCount.get());
        assertEquals(2, component.transformCount.get());
        assertEquals(1, component.modelBuildCount.get());
        for (int index = 0; index < 2; index++) {
            String prompt = model.inputAt(index).stream()
                    .filter(message -> message.getRole() == MsgRole.SYSTEM)
                    .findFirst()
                    .orElseThrow()
                    .getTextContent();
            assertTrue(prompt.contains("stable build prompt"));
            assertEquals(1, occurrences(prompt, "dynamic conversation-7"));
        }
    }

    @Test
    void emptyDynamicPromptFailsBeforeModelCall() {
        configureAgent();
        Slot slot = slot("empty-prompt-request");
        ScriptedChatModel model = new ScriptedChatModel("must not run");
        TestComponent component = component(slot, model);
        component.emptyTransformedPrompt = true;

        RuntimeException thrown = assertThrows(RuntimeException.class, component::process);

        assertTrue(hasCause(thrown, AgentConfigException.class));
        assertEquals(1, component.transformCount.get());
        assertEquals(0, model.callCount());
    }

    @Test
    void mutuallyExclusiveStructuredModesFailBeforeRuntimeAndModelCreation() {
        configureAgent();
        Slot slot = slot("invalid-output-request");
        ScriptedChatModel model = new ScriptedChatModel("must not run");
        TestComponent component = component(slot, model);
        component.outputType = StructuredReply.class;
        component.outputSchema = schema();

        assertThrows(AgentConfigException.class, component::process);

        assertEquals(0, component.modelBuildCount.get());
        assertEquals(0, model.callCount());
    }

    @Test
    void strictStateStoreFailurePrecedesPrivatePromptTransformAndModelEntry() {
        configureAgent().getStateStore().setFailurePolicy(
                AgentStateStoreFailurePolicy.FAIL_FAST);
        RuntimeException loadFailure = new RuntimeException("state backend unavailable");
        ScriptedChatModel model = new ScriptedChatModel("must not run");
        TestComponent component = component(slot("strict-state-request"), model);
        component.stateLoadFailure = loadFailure;
        component.transformFailure = new RuntimeException("transform must not run");

        RuntimeException thrown = assertThrows(RuntimeException.class, component::process);

        assertTrue(hasCause(thrown, loadFailure));
        assertEquals(0, component.transformCount.get());
        assertEquals(0, model.callCount());
    }

    @Test
    void upstreamStateLoadFailureStopsBeforePromptTransformEvenWithLogPolicy()
            throws Exception {
        configureAgent().getStateStore().setFailurePolicy(
                AgentStateStoreFailurePolicy.LOG_AND_CONTINUE);
        ScriptedChatModel model = new ScriptedChatModel("continued reply");
        Slot slot = slot("non-strict-state-request");
        TestComponent component = component(slot, model);
        component.stateLoadFailure = new RuntimeException("state backend unavailable");

        RuntimeException thrown = assertThrows(RuntimeException.class, component::process);

        assertTrue(hasCause(thrown, component.stateLoadFailure));
        assertEquals(0, component.transformCount.get());
        assertEquals(0, model.callCount());
    }

    private TestComponent component(Slot slot, ScriptedChatModel model) {
        TestComponent component = new TestComponent(slot, model);
        component.setNodeId("agent");
        components.add(component);
        return component;
    }

    private AgentConfig configureAgent() {
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getStateStore().setJsonRoot("target/agent-state");
        agentConfig.getRuntime().setNamespace("structured-test");
        agentConfig.getRuntime().setDefaultUserId("test-user");
        agentConfig.getRuntime().setTimeout(Duration.ofSeconds(2));
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agentConfig);
        LiteflowConfigGetter.setLiteflowConfig(config);
        return agentConfig;
    }

    private static Slot slot(String requestId) {
        Slot slot = new Slot();
        slot.setChainId("structured-chain");
        slot.setConversationId("conversation-7");
        slot.putRequestId(requestId);
        return slot;
    }

    private static JsonNode schema() {
        return OBJECT_MAPPER.createObjectNode()
                .put("type", "object")
                .set("properties", OBJECT_MAPPER.createObjectNode()
                        .set("answer", OBJECT_MAPPER.createObjectNode().put("type", "string")));
    }

    private static int occurrences(String value, String needle) {
        return value.split(needle, -1).length - 1;
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCause(Throwable failure, Throwable expected) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current == expected) {
                return true;
            }
        }
        return false;
    }

    public static final class StructuredReply {
        public String answer;
    }

    private static final class TestComponent extends AgentComponent {
        private final Slot slot;
        private final ScriptedChatModel model;
        private final AtomicInteger modelBuildCount = new AtomicInteger();
        private final AtomicInteger systemPromptCount = new AtomicInteger();
        private final AtomicInteger transformCount = new AtomicInteger();
        private Class<?> outputType;
        private JsonNode outputSchema;
        private boolean emptyTransformedPrompt;
        private RuntimeException transformFailure;
        private RuntimeException stateLoadFailure;

        private TestComponent(Slot slot, ScriptedChatModel model) {
            this.slot = slot;
            this.model = model;
        }

        @Override
        public Slot getSlot() {
            return slot;
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
        protected String systemPrompt() {
            systemPromptCount.incrementAndGet();
            return "stable build prompt";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            assertEquals(slot, context.getSlot());
            return "question";
        }

        @Override
        protected Mono<String> transformSystemPrompt(
                String currentPrompt, LiteFlowAgentContext context) {
            return Mono.defer(() -> {
                transformCount.incrementAndGet();
                if (transformFailure != null) {
                    return Mono.error(transformFailure);
                }
                if (emptyTransformedPrompt) {
                    return Mono.empty();
                }
                return Mono.just(currentPrompt + "\ndynamic " + context.getConversationId());
            });
        }

        @Override
        protected AgentStateStoreResolver stateStoreResolver() {
            if (stateLoadFailure == null) {
                return super.stateStoreResolver();
            }
            return ignored -> new ResolvedAgentStateStore(
                    new LoadFailingStore(stateLoadFailure), false);
        }

        @Override
        protected Class<?> structuredOutputType() {
            return outputType;
        }

        @Override
        protected JsonNode structuredOutputSchema() {
            return outputSchema;
        }
    }

    private static final class LoadFailingStore implements AgentStateStore {
        private final RuntimeException failure;

        private LoadFailingStore(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void save(String userId, String sessionId, String key, State value) {
        }

        @Override
        public void save(
                String userId, String sessionId, String key, List<? extends State> values) {
        }

        @Override
        public <T extends State> Optional<T> get(
                String userId, String sessionId, String key, Class<T> type) {
            throw failure;
        }

        @Override
        public <T extends State> List<T> getList(
                String userId, String sessionId, String key, Class<T> itemType) {
            return List.of();
        }

        @Override
        public boolean exists(String userId, String sessionId) {
            return false;
        }

        @Override
        public void delete(String userId, String sessionId) {
        }

        @Override
        public Set<String> listSessionIds(String userId) {
            return Set.of();
        }
    }
}
