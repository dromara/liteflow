package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.exception.AgentException;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.testsupport.ScriptedChatModel;
import com.yomahub.liteflow.property.agent.AgentStateStoreFailurePolicy;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.ReasoningInput;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateStoreFailureMiddlewareTest {

    private static final String NAMESPACE = "lf-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String SESSION = "lf-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test
    void failFastStopsAfterAgentScopeSwallowsLoadFailureAndBeforeModelCall() {
        RuntimeException loadFailure = new RuntimeException("state backend unavailable");
        GuardedNamespacedAgentStateStore store = new GuardedNamespacedAgentStateStore(
                new LoadFailingStore(loadFailure), NAMESPACE);
        ScriptedChatModel model = new ScriptedChatModel("must-not-run");
        StateStoreFailureMiddleware middleware = new StateStoreFailureMiddleware(
                store, AgentStateStoreFailurePolicy.FAIL_FAST, warning -> { });
        ReActAgent agent = buildAgent(store, model, middleware);
        RuntimeContext context = RuntimeContext.builder()
                .userId("alice")
                .sessionId(SESSION)
                .build();

        try {
            AgentException thrown = assertThrows(AgentException.class,
                    () -> agent.call(List.of(new UserMessage("hello")), context).block());
            assertSame(loadFailure, thrown.getCause());
            assertEquals(0, model.callCount());
            assertTrue(store.takeLoadFailure("alice", SESSION).isEmpty());
            assertEquals(Integer.MAX_VALUE, middleware.order());
        } finally {
            agent.close();
        }
    }

    @Test
    void logAndContinueEmitsWarningClearsFailureAndRunsModel() {
        RuntimeException loadFailure = new RuntimeException("state backend unavailable");
        GuardedNamespacedAgentStateStore store = new GuardedNamespacedAgentStateStore(
                new LoadFailingStore(loadFailure), NAMESPACE);
        ScriptedChatModel model = new ScriptedChatModel("continued reply");
        List<String> warnings = new ArrayList<>();
        StateStoreFailureMiddleware middleware = new StateStoreFailureMiddleware(
                store, AgentStateStoreFailurePolicy.LOG_AND_CONTINUE, warnings::add);
        ReActAgent agent = buildAgent(store, model, middleware);
        RuntimeContext context = RuntimeContext.builder()
                .userId("alice")
                .sessionId(SESSION)
                .build();

        try {
            Msg reply = agent.call(List.of(new UserMessage("hello")), context).block();

            assertNotNull(reply);
            assertEquals("continued reply", reply.getTextContent());
            assertEquals(1, model.callCount());
            assertEquals(1, warnings.size());
            assertTrue(warnings.get(0).contains("state backend unavailable"));
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
                store, AgentStateStoreFailurePolicy.FAIL_FAST, warning -> { });
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
            StateStoreFailureMiddleware middleware) {
        return ReActAgent.builder()
                .name("state-failure-test")
                .sysPrompt("Answer directly.")
                .model(model)
                .toolkit(new Toolkit())
                .maxIters(2)
                .stateStore(store)
                .middleware(middleware)
                .build();
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
