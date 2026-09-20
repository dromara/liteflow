package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.testsupport.ScriptedChatModel;
import com.yomahub.liteflow.property.agent.AgentSessionStoreFailurePolicy;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ReasoningInput;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateStoreFailureMiddlewareTest {

    private static final String NAMESPACE = "lf-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String SESSION = "lf-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test
    void upstreamLoadFailureStopsBeforeModelCall() {
        RuntimeException loadFailure = new RuntimeException("state backend unavailable");
        GuardedNamespacedAgentStateStore store = new GuardedNamespacedAgentStateStore(
                new LoadFailingStore(loadFailure), NAMESPACE);
        ScriptedChatModel model = new ScriptedChatModel("must-not-run");
        StateStoreFailureMiddleware middleware = new StateStoreFailureMiddleware(
                store, AgentSessionStoreFailurePolicy.FAIL_FAST, warning -> { });
        RuntimeException transformFailure = new RuntimeException("transform must not run");
        PromptTransformMiddleware transform = new PromptTransformMiddleware(
                new ArrayList<>(), transformFailure);
        ReActAgent agent = buildAgent(store, model, middleware, transform);
        RuntimeContext context = RuntimeContext.builder()
                .userId("alice")
                .sessionId(SESSION)
                .build();

        try {
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> agent.call(List.of(new UserMessage("hello")), context).block());
            assertTrue(hasCause(thrown, loadFailure));
            assertEquals(0, transform.callCount.get());
            assertEquals(0, model.callCount());
            assertTrue(store.takeLoadFailure("alice", SESSION).isEmpty());
            assertEquals(10_000, middleware.order());
        } finally {
            agent.close();
        }
    }

    @Test
    void logPolicyDoesNotSwallowUpstreamLoadFailure() {
        RuntimeException loadFailure = new RuntimeException("state backend unavailable");
        GuardedNamespacedAgentStateStore store = new GuardedNamespacedAgentStateStore(
                new LoadFailingStore(loadFailure), NAMESPACE);
        ScriptedChatModel model = new ScriptedChatModel("continued reply");
        List<String> warnings = new ArrayList<>();
        List<String> events = new ArrayList<>();
        StateStoreFailureMiddleware middleware = new StateStoreFailureMiddleware(
                store, AgentSessionStoreFailurePolicy.LOG_AND_CONTINUE, warning -> {
                    warnings.add(warning);
                    events.add("warning");
                });
        PromptTransformMiddleware transform = new PromptTransformMiddleware(events, null);
        ReActAgent agent = buildAgent(store, model, middleware, transform);
        RuntimeContext context = RuntimeContext.builder()
                .userId("alice")
                .sessionId(SESSION)
                .build();

        try {
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> agent.call(List.of(new UserMessage("hello")), context).block());

            assertTrue(hasCause(thrown, loadFailure));
            assertTrue(events.isEmpty());
            assertEquals(0, transform.callCount.get());
            assertEquals(0, model.callCount());
            assertTrue(warnings.isEmpty());
            assertTrue(store.takeLoadFailure("alice", SESSION).isEmpty());
        } finally {
            agent.close();
        }
    }

    @Test
    void onAgentClearsFailureWhenLifecycleEndsBeforeInnerMiddlewareHooks() {
        RuntimeException loadFailure = new RuntimeException("state backend unavailable");
        GuardedNamespacedAgentStateStore store = new GuardedNamespacedAgentStateStore(
                new LoadFailingStore(loadFailure), NAMESPACE);
        StateStoreFailureMiddleware middleware = new StateStoreFailureMiddleware(
                store, AgentSessionStoreFailurePolicy.FAIL_FAST, warning -> { });
        RuntimeContext context = RuntimeContext.builder()
                .userId("alice")
                .sessionId(SESSION)
                .build();

        middleware.onAgent(null, context, new AgentInput(List.of()), input -> {
            assertThrows(RuntimeException.class,
                    () -> store.get("alice", SESSION, "state", UserMessage.class));
            return Flux.empty();
        }).blockLast();

        ReasoningInput laterReasoning = new ReasoningInput(List.of(), List.of(), null);
        assertDoesNotThrow(() -> middleware.onReasoning(
                null, context, laterReasoning, ignored -> Flux.empty()).blockLast());
        assertTrue(store.takeLoadFailure("alice", SESSION).isEmpty());
    }

    private static ReActAgent buildAgent(
            AgentStateStore store,
            ScriptedChatModel model,
            MiddlewareBase... middlewares) {
        return ReActAgent.builder()
                .name("state-failure-test")
                .sysPrompt("Answer directly.")
                .model(model)
                .toolkit(new Toolkit())
                .maxIters(2)
                .stateStore(store)
                .middlewares(List.of(middlewares))
                .build();
    }

    private static boolean hasCause(Throwable failure, Throwable expected) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current == expected) {
                return true;
            }
        }
        return false;
    }

    private static final class PromptTransformMiddleware implements MiddlewareBase {
        private final List<String> events;
        private final RuntimeException failure;
        private final AtomicInteger callCount = new AtomicInteger();

        private PromptTransformMiddleware(List<String> events, RuntimeException failure) {
            this.events = events;
            this.failure = failure;
        }

        @Override
        public Mono<String> onSystemPrompt(
                Agent agent, RuntimeContext context, String currentPrompt) {
            callCount.incrementAndGet();
            events.add("transform");
            if (failure != null) {
                return Mono.error(failure);
            }
            return Mono.just(currentPrompt + " transformed");
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
        public void save(String userId, String sessionId, String key, List<? extends State> values) {
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
