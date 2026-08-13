package com.yomahub.liteflow.agent.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.MessageMetadataKeys;
import io.agentscope.core.message.Msg;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractAgentComponentTest {

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
    void processCreatesIsolatedExplicitContextsAfterRuntimeConstruction() throws Exception {
        configureAgent();
        Slot slot = slot();
        TestComponent component = component(slot);
        component.reply = Mono.just(AssistantMessage.builder().textContent("answer").build());

        component.process();
        component.process();

        assertTrue(Modifier.isFinal(AbstractAgentComponent.class
                .getMethod("process").getModifiers()));
        assertTrue(Modifier.isFinal(AbstractAgentComponent.class
                .getMethod("close").getModifiers()));
        assertEquals(1, component.buildCount.get());
        assertTrue(component.events.indexOf("buildRuntime")
                < component.events.indexOf("customizeRuntimeContext"));
        assertEquals(List.of(false), component.contextWasExposedDuringBuild);

        LiteFlowAgentContext first = component.contexts.get(0);
        LiteFlowAgentContext second = component.contexts.get(1);
        assertNotSame(first, second);
        assertNotSame(component.runtimeContexts.get(0), component.runtimeContexts.get(1));
        assertSame(first, component.runtimeContexts.get(0).get(LiteFlowAgentContext.class));
        assertSame(slot, component.runtimeContexts.get(0).get(Slot.class));
        assertEquals("test-user", component.runtimeContexts.get(0).getUserId());
        assertEquals(first.getRuntimeSessionId(), component.runtimeContexts.get(0).getSessionId());
        assertEquals("conversation-7", first.getConversationId());
        assertEquals("plain-chain", first.getChainId());
        assertEquals("abstract-agent", first.getNodeId());
        assertEquals("request-1", first.getRequestId());
        assertEquals(first.getRequestId(), first.getTraceId());
        assertNotNull(first.getDeadline());
        assertTrue(first.getRuntimeSessionId().matches("lf-[0-9a-f]{64}"));
        assertTrue(first.getAgentNamespace().matches("lf-[0-9a-f]{64}"));
        assertEquals(AgentOutputSpec.text(), first.getOutputSpec());
        assertEquals(List.of(true, true), component.attachmentVisibleDuringInvoke);
        assertFalse(slot.hasAttachment(first.getAttachmentKey()));
        assertFalse(slot.hasAttachment(second.getAttachmentKey()));
        assertNotEquals(first.getAttachmentKey(), second.getAttachmentKey());
        first.recordUsedSkill("one");
        List<String> usedSkills = first.getUsedSkills();
        first.recordUsedSkill("two");
        assertEquals(List.of("one"), usedSkills);
        assertThrows(UnsupportedOperationException.class, () -> usedSkills.add("three"));
    }

    @Test
    void validatesOutputModeBeforeBuildingOrInvokingRuntime() {
        configureAgent();
        TestComponent component = component(slot());
        component.outputType = StructuredReply.class;
        component.outputSchema = schema();

        AgentConfigException thrown = assertThrows(
                AgentConfigException.class, component::process);

        assertTrue(thrown.getMessage().contains("mutually exclusive"));
        assertEquals(0, component.buildCount.get());
        assertEquals(0, component.invokeCount.get());
    }

    @Test
    void clearsOldResponseForNullReplyAndEveryReplyExtractionFailure() throws Exception {
        configureAgent();
        Slot slot = slot();
        slot.setResponseData("stale");
        TestComponent nullReply = component(slot);
        nullReply.reply = Mono.empty();

        nullReply.process();

        assertNull(slot.getResponseData());
        assertFalse(slot.hasAttachment(nullReply.contexts.get(0).getAttachmentKey()));

        slot.setResponseData("stale-again");
        TestComponent malformedStructuredReply = component(slot);
        malformedStructuredReply.outputType = StructuredReply.class;
        malformedStructuredReply.reply = Mono.just(
                AssistantMessage.builder().textContent("not structured").build());

        AgentInvocationException thrown = assertThrows(
                AgentInvocationException.class, malformedStructuredReply::process);

        assertEquals(AgentInvocationErrorType.STRUCTURED_OUTPUT, thrown.getErrorType());
        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertNull(slot.getResponseData());
        assertFalse(slot.hasAttachment(
                malformedStructuredReply.contexts.get(0).getAttachmentKey()));
    }

    @Test
    void cleansOnlyItsOwnAttachmentOnBuildAndInvocationFailures() throws Exception {
        configureAgent();
        Slot slot = slot();
        slot.setAttachment("unrelated", "keep");
        TestComponent buildFailure = component(slot);
        buildFailure.buildFailure = new IllegalStateException("build failed");

        assertThrows(IllegalStateException.class, buildFailure::process);
        assertEquals(Set.of(), agentAttachmentKeys(slot));
        assertEquals("keep", slot.getAttachment("unrelated"));

        TestComponent customizationFailure = component(slot);
        customizationFailure.customizationFailure =
                new IllegalStateException("customization failed");
        assertThrows(IllegalStateException.class, customizationFailure::process);
        assertEquals(Set.of(), agentAttachmentKeys(slot));

        TestComponent promptFailure = component(slot);
        promptFailure.promptFailure = new IllegalStateException("prompt failed");
        assertThrows(IllegalStateException.class, promptFailure::process);
        assertEquals(Set.of(), agentAttachmentKeys(slot));
        assertEquals("keep", slot.getAttachment("unrelated"));

        TestComponent replacedAttachment = component(slot);
        replacedAttachment.replaceAttachmentDuringInvoke = true;
        replacedAttachment.process();
        String replacedKey = replacedAttachment.contexts.get(0).getAttachmentKey();
        assertEquals("replacement", slot.getAttachment(replacedKey));
        slot.removeAttachment(replacedKey);

        TestComponent invocationFailure = component(slot);
        invocationFailure.reply = Mono.error(new IllegalArgumentException("invoke failed"));

        assertThrows(RuntimeException.class, invocationFailure::process);
        assertFalse(slot.hasAttachment(invocationFailure.contexts.get(0).getAttachmentKey()));
        assertEquals(Set.of(), agentAttachmentKeys(slot));
        assertEquals("keep", slot.getAttachment("unrelated"));
    }

    @Test
    void timeoutCancelsInvocationAndCleansItsAttachment() throws Exception {
        AgentConfig config = configureAgent();
        config.getRuntime().setTimeout(Duration.ofMillis(25));
        Slot slot = slot();
        TestComponent component = component(slot);
        component.reply = Mono.never();

        AgentInvocationException thrown = assertThrows(
                AgentInvocationException.class, component::process);

        assertEquals(AgentInvocationErrorType.TIMEOUT, thrown.getErrorType());
        LiteFlowAgentContext context = component.contexts.get(0);
        assertTrue(context.isCancelled());
        assertFalse(slot.hasAttachment(context.getAttachmentKey()));
    }

    @Test
    void closeWaitsForActiveInvocationBeforeClosingRuntime() throws Exception {
        AgentConfig config = configureAgent();
        config.getRuntime().setTimeout(Duration.ofSeconds(10));
        TestComponent component = component(slot());
        CountDownLatch invocationEntered = new CountDownLatch(1);
        CountDownLatch releaseInvocation = new CountDownLatch(1);
        CountDownLatch closeStarted = new CountDownLatch(1);
        CountDownLatch closeFinished = new CountDownLatch(1);
        component.reply = Mono.fromCallable(() -> {
            invocationEntered.countDown();
            assertTrue(releaseInvocation.await(5, TimeUnit.SECONDS));
            return AssistantMessage.builder().textContent("answer").build();
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> invocation = executor.submit(() -> {
                component.process();
                return null;
            });
            assertTrue(invocationEntered.await(5, TimeUnit.SECONDS));
            Future<?> close = executor.submit(() -> {
                closeStarted.countDown();
                component.close();
                closeFinished.countDown();
                return null;
            });
            assertTrue(closeStarted.await(5, TimeUnit.SECONDS));

            assertFalse(closeFinished.await(200, TimeUnit.MILLISECONDS));
            assertEquals(0, component.runtime.get().closeCount.get());

            releaseInvocation.countDown();
            invocation.get(5, TimeUnit.SECONDS);
            close.get(5, TimeUnit.SECONDS);
            assertEquals(1, component.runtime.get().closeCount.get());
            assertThrows(IllegalStateException.class, component::process);
        } finally {
            releaseInvocation.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void differentInvocationsCanUseOneComponentRuntimeConcurrently() throws Exception {
        AgentConfig config = configureAgent();
        config.getRuntime().setTimeout(Duration.ofSeconds(10));
        TestComponent component = component(slot());
        component.distinctConversationPerCall = true;
        CountDownLatch bothInvocationsEntered = new CountDownLatch(2);
        CountDownLatch releaseInvocations = new CountDownLatch(1);
        AtomicInteger activeInvocations = new AtomicInteger();
        AtomicInteger maximumActiveInvocations = new AtomicInteger();
        component.reply = Mono.fromCallable(() -> {
            int active = activeInvocations.incrementAndGet();
            maximumActiveInvocations.accumulateAndGet(active, Math::max);
            bothInvocationsEntered.countDown();
            try {
                assertTrue(releaseInvocations.await(5, TimeUnit.SECONDS));
                return AssistantMessage.builder().textContent("answer").build();
            } finally {
                activeInvocations.decrementAndGet();
            }
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> {
                component.process();
                return null;
            });
            Future<?> second = executor.submit(() -> {
                component.process();
                return null;
            });

            assertTrue(bothInvocationsEntered.await(5, TimeUnit.SECONDS));
            assertEquals(2, maximumActiveInvocations.get());
            releaseInvocations.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            releaseInvocations.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void closeFromInvocationHookIsRejectedWithoutClosingRuntime() {
        configureAgent();
        TestComponent component = component(slot());
        component.closeDuringInvoke = true;

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class, component::process);

        assertTrue(thrown.getMessage().contains("active invocation"));
        assertEquals(0, component.runtime.get().closeCount.get());
    }

    private TestComponent component(Slot slot) {
        TestComponent component = new TestComponent(slot);
        component.setNodeId("abstract-agent");
        components.add(component);
        return component;
    }

    private AgentConfig configureAgent() {
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getRuntime().setNamespace("abstract-test");
        agentConfig.getRuntime().setDefaultUserId("test-user");
        agentConfig.getRuntime().setTimeout(Duration.ofSeconds(1));
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agentConfig);
        LiteflowConfigGetter.setLiteflowConfig(config);
        return agentConfig;
    }

    private static Slot slot() {
        Slot slot = new Slot();
        slot.setChainId("plain-chain");
        slot.setConversationId("conversation-7");
        slot.putRequestId("request-1");
        return slot;
    }

    private static JsonNode schema() {
        return OBJECT_MAPPER.createObjectNode()
                .put("type", "object")
                .set("properties", OBJECT_MAPPER.createObjectNode()
                        .set("answer", OBJECT_MAPPER.createObjectNode().put("type", "string")));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> agentAttachmentKeys(Slot slot) throws Exception {
        Field field = Slot.class.getDeclaredField("metaDataMap");
        field.setAccessible(true);
        Map<String, Object> metadata = (ConcurrentHashMap<String, Object>) field.get(slot);
        return metadata.keySet().stream()
                .filter(key -> key.startsWith(LiteFlowAgentContext.SLOT_ATTACHMENT_PREFIX))
                .collect(Collectors.toSet());
    }

    public static final class StructuredReply {
        public String answer;
    }

    private static final class TestRuntime implements AutoCloseable {
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    private static class TestComponent extends AbstractAgentComponent<TestRuntime> {
        private final Slot slot;
        private final AtomicInteger buildCount = new AtomicInteger();
        private final AtomicInteger invokeCount = new AtomicInteger();
        private final AtomicInteger conversationSequence = new AtomicInteger();
        private final AtomicReference<TestRuntime> runtime = new AtomicReference<>();
        private final List<String> events = new CopyOnWriteArrayList<>();
        private final List<Boolean> contextWasExposedDuringBuild = new CopyOnWriteArrayList<>();
        private final List<Boolean> attachmentVisibleDuringInvoke = new CopyOnWriteArrayList<>();
        private final List<LiteFlowAgentContext> contexts = new CopyOnWriteArrayList<>();
        private final List<RuntimeContext> runtimeContexts = new CopyOnWriteArrayList<>();
        private Mono<Msg> reply = Mono.just(AssistantMessage.builder()
                .metadata(Map.of(MessageMetadataKeys.STRUCTURED_OUTPUT,
                        Map.of("answer", "structured")))
                .build());
        private Class<?> outputType;
        private JsonNode outputSchema;
        private RuntimeException buildFailure;
        private RuntimeException customizationFailure;
        private RuntimeException promptFailure;
        private boolean replaceAttachmentDuringInvoke;
        private boolean distinctConversationPerCall;
        private boolean closeDuringInvoke;

        private TestComponent(Slot slot) {
            this.slot = slot;
        }

        @Override
        public Slot getSlot() {
            return slot;
        }

        @Override
        protected String resolveConversationId(Slot slot) {
            if (distinctConversationPerCall) {
                return "parallel-conversation-" + conversationSequence.incrementAndGet();
            }
            return super.resolveConversationId(slot);
        }

        @Override
        protected TestRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
            events.add("buildRuntime");
            buildCount.incrementAndGet();
            contextWasExposedDuringBuild.add(!contexts.isEmpty());
            if (buildFailure != null) {
                throw buildFailure;
            }
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
            events.add("invokeRuntime");
            invokeCount.incrementAndGet();
            contexts.add(liteflowContext);
            runtimeContexts.add(runtimeContext);
            attachmentVisibleDuringInvoke.add(
                    slot.getAttachment(liteflowContext.getAttachmentKey()) == liteflowContext);
            if (replaceAttachmentDuringInvoke) {
                slot.setAttachment(liteflowContext.getAttachmentKey(), "replacement");
            }
            if (closeDuringInvoke) {
                close();
            }
            return reply;
        }

        @Override
        protected String systemPrompt() {
            return "base";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            events.add("userPrompt");
            if (promptFailure != null) {
                throw promptFailure;
            }
            return "question";
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
        protected void customizeRuntimeContext(
                RuntimeContext.Builder builder, LiteFlowAgentContext context) {
            events.add("customizeRuntimeContext");
            if (customizationFailure != null) {
                throw customizationFailure;
            }
            builder.put("custom", "value");
        }
    }
}
