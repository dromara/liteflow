package com.yomahub.liteflow.agent.a2a.server;

import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.a2a.server.AgentScopeA2aServer;
import io.agentscope.core.a2a.server.card.ConfigurableAgentCard;
import io.agentscope.core.a2a.server.executor.runner.AgentRequestOptions;
import io.agentscope.core.a2a.server.transport.TransportProperties;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class LiteFlowA2aAgentRunnerTest {

    @Test
    void resolvesRemoteIdentityBeforeOpeningOneRuntimePerTask() {
        RecordingFactory factory = new RecordingFactory();
        LiteFlowA2aAgentRunner runner = new LiteFlowA2aAgentRunner(
                factory, "orders", "public-support", "guest");

        runner.stream(List.of(new UserMessage("hello")), options("task-1", "session/raw", null));

        AgentRequestOptions opened = factory.opened.get(0);
        String expectedSession = new InvocationIdentityResolver("orders")
                .resolve("guest", "session/raw", "public-support")
                .runtimeSessionId();
        assertEquals("task-1", opened.getTaskId());
        assertEquals("guest", opened.getUserId());
        assertEquals(expectedSession, opened.getSessionId());
        assertEquals("support", runner.getAgentName());
        assertEquals("Support agent", runner.getAgentDescription());
    }

    @Test
    void rejectsMissingStableIdsBeforeOpeningRuntime() {
        RecordingFactory factory = new RecordingFactory();
        LiteFlowA2aAgentRunner runner = new LiteFlowA2aAgentRunner(
                factory, "orders", "public-support", "guest");

        assertThrows(IllegalArgumentException.class,
                () -> runner.stream(List.of(), options("task-1", " ", "user-1")));
        assertThrows(IllegalArgumentException.class,
                () -> runner.stream(List.of(), options(" ", "session-1", "user-1")));
        assertEquals(0, factory.opened.size());
    }

    @Test
    void rejectsDuplicateActiveTaskBeforeOpeningAnotherRuntime() {
        RecordingFactory factory = new RecordingFactory();
        LiteFlowA2aAgentRunner runner = new LiteFlowA2aAgentRunner(
                factory, "orders", "public-support", "guest");
        AgentRequestOptions request = options("task-1", "session-1", "user-1");

        runner.stream(List.of(), request);
        IllegalStateException duplicate = assertThrows(
                IllegalStateException.class, () -> runner.stream(List.of(), request));

        assertEquals("A2A task is already active: task-1", duplicate.getMessage());
        assertEquals(1, factory.opened.size());
        runner.stop("task-1");
        assertEquals(1, factory.interrupts.get());
        assertEquals(1, factory.closes.get());
    }

    @Test
    void ownedReActRuntimeUsesTypedEventsAndAllowlistedStringMetadataOnly() {
        ReActAgent agent = mock(ReActAgent.class);
        when(agent.streamEvents(anyList(), any(RuntimeContext.class)))
                .thenReturn(Flux.just(new AgentResultEvent(new AssistantMessage("done"))));
        A2aServerAgentFactory factory = A2aServerAgentFactory.forReActAgent(
                "support", "Support agent", ignored -> agent);
        LiteFlowA2aAgentRunner runner = new LiteFlowA2aAgentRunner(
                factory, "orders", "public-support", "guest");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("liteflow.traceId", "trace-7");
        metadata.put("liteflow.tenantId", "tenant-3");
        metadata.put("authorization", "Bearer secret");
        metadata.put("apiKey", "secret");
        metadata.put("unknown", "not-allowed");
        metadata.put("liteflow.traceId.object", new Object());
        UserMessage message = UserMessage.builder()
                .textContent("hello")
                .metadata(metadata)
                .build();

        runner.stream(List.of(message), options("task-1", "session-1", "user-1"))
                .collectList()
                .block();

        ArgumentCaptor<RuntimeContext> contextCaptor =
                ArgumentCaptor.forClass(RuntimeContext.class);
        verify(agent).streamEvents(anyList(), contextCaptor.capture());
        RuntimeContext context = contextCaptor.getValue();
        String expectedSession = new InvocationIdentityResolver("orders")
                .resolve("user-1", "session-1", "public-support")
                .runtimeSessionId();
        assertEquals("user-1", context.getUserId());
        assertEquals(expectedSession, context.getSessionId());
        assertEquals("task-1", context.get(AgentEvent.METADATA_TASK_ID));
        assertEquals(Map.of(
                AgentEvent.METADATA_TASK_ID, "task-1",
                "liteflow.traceId", "trace-7",
                "liteflow.tenantId", "tenant-3"), context.getExtra());
        verify(agent).close();
        verify(agent, never()).interrupt(any(RuntimeContext.class));
    }

    @Test
    void buildsProtocolServerWithoutStartingOrPublishingAnEndpoint() {
        RecordingFactory runtimeFactory = new RecordingFactory();
        LiteFlowA2aAgentRunner runner = new LiteFlowA2aAgentRunner(
                runtimeFactory, "orders", "public-support", "guest");
        ConfigurableAgentCard card = new ConfigurableAgentCard.Builder()
                .name("support")
                .description("Support agent")
                .version("1.0")
                .build();
        TransportProperties transport = TransportProperties.builder("JSONRPC")
                .host("127.0.0.1")
                .port(8099)
                .path("/a2a")
                .build();

        AgentScopeA2aServer server = new LiteFlowA2aServerFactory()
                .create(runner, card, transport);

        assertEquals("support", server.getAgentCard().name());
        assertEquals("http://127.0.0.1:8099/a2a", server.getAgentCard().url());
    }

    private static AgentRequestOptions options(
            String taskId, String sessionId, String userId) {
        AgentRequestOptions options = new AgentRequestOptions();
        options.setTaskId(taskId);
        options.setSessionId(sessionId);
        options.setUserId(userId);
        return options;
    }

    private static final class RecordingFactory implements A2aServerAgentFactory {
        private final List<AgentRequestOptions> opened = new CopyOnWriteArrayList<>();
        private final AtomicInteger interrupts = new AtomicInteger();
        private final AtomicInteger closes = new AtomicInteger();

        @Override
        public String agentName() {
            return "support";
        }

        @Override
        public String agentDescription() {
            return "Support agent";
        }

        @Override
        public OwnedAgentRuntime open(AgentRequestOptions options) {
            opened.add(options);
            return new OwnedAgentRuntime() {
                @Override
                public Flux<AgentEvent> stream(List<Msg> messages) {
                    return Flux.never();
                }

                @Override
                public void interrupt() {
                    interrupts.incrementAndGet();
                }

                @Override
                public void close() {
                    closes.incrementAndGet();
                }
            };
        }
    }
}
