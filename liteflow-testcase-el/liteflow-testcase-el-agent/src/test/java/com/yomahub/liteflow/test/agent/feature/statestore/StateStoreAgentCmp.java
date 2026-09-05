package com.yomahub.liteflow.test.agent.feature.statestore;

import com.yomahub.liteflow.agent.component.AgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.test.agent.support.DeterministicHistoryModel;
import com.yomahub.liteflow.test.agent.support.ScriptedChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.state.State;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class StateStoreAgentCmp extends AgentComponent {

    private static final RecordingStore STORE = new RecordingStore();
    private static final List<Integer> ALPHA_COUNTS = new CopyOnWriteArrayList<>();
    private static final List<Integer> BETA_COUNTS = new CopyOnWriteArrayList<>();

    static void reset() {
        STORE.resetObservations();
        ALPHA_COUNTS.clear();
        BETA_COUNTS.clear();
    }

    static List<Integer> alphaMessageCounts() {
        return List.copyOf(ALPHA_COUNTS);
    }

    static List<Integer> betaMessageCounts() {
        return List.copyOf(BETA_COUNTS);
    }

    static Set<String> physicalSessions() {
        return STORE.physicalSessions();
    }

    abstract List<Integer> messageCounts();

    @Override
    protected final ModelSpec<?> model() {
        throw new AssertionError("offline buildModel must be used");
    }

    @Override
    protected final Model buildModel() {
        return new DeterministicHistoryModel(getNodeId() + "-offline-model", messageCounts());
    }

    @Override
    protected final AgentStateStoreResolver stateStoreResolver() {
        return ignored -> new ResolvedAgentStateStore(STORE, false);
    }

    @Override
    protected final String systemPrompt() {
        return "offline state-store contract";
    }

    @Override
    protected final String userPrompt(LiteFlowAgentContext context) {
        Object request = context.getSlot().getChainReqData(context.getChainId());
        return request == null ? "" : request.toString();
    }

    @Component("stateAlphaAgent")
    static final class Alpha extends StateStoreAgentCmp {
        @Override
        List<Integer> messageCounts() {
            return ALPHA_COUNTS;
        }

        @Override
        protected String agentKey() {
            return "state-alpha";
        }
    }

    @Component("stateBetaAgent")
    static final class Beta extends StateStoreAgentCmp {
        @Override
        List<Integer> messageCounts() {
            return BETA_COUNTS;
        }

        @Override
        protected String agentKey() {
            return "state-beta";
        }
    }

    private static final class RecordingStore extends InMemoryAgentStateStore {
        private final Set<String> sessions = ConcurrentHashMap.newKeySet();

        @Override
        public void save(String userId, String sessionId, String key, State value) {
            sessions.add(sessionId);
            super.save(userId, sessionId, key, value);
        }

        @Override
        public void save(
                String userId, String sessionId, String key, List<? extends State> values) {
            sessions.add(sessionId);
            super.save(userId, sessionId, key, values);
        }

        Set<String> physicalSessions() {
            return Set.copyOf(sessions);
        }

        void resetObservations() {
            sessions.clear();
        }
    }
}

@Component("failingStateAgent")
final class FailingStateStoreAgentCmp extends AgentComponent {

    private static final AtomicInteger MODEL_CALLS = new AtomicInteger();

    static void reset() {
        MODEL_CALLS.set(0);
    }

    static int modelCalls() {
        return MODEL_CALLS.get();
    }

    @Override
    protected ModelSpec<?> model() {
        throw new AssertionError("offline buildModel must be used");
    }

    @Override
    protected Model buildModel() {
        return ScriptedChatModel.builder()
                .observeCalls(ignored -> MODEL_CALLS.incrementAndGet())
                .reply("must-not-run")
                .build();
    }

    @Override
    protected AgentStateStoreResolver stateStoreResolver() {
        return ignored -> new ResolvedAgentStateStore(new InMemoryAgentStateStore() {
            @Override
            public <T extends State> Optional<T> get(
                    String userId, String sessionId, String key, Class<T> type) {
                throw new IllegalStateException("offline state load failed");
            }
        }, false);
    }

    @Override
    protected String systemPrompt() {
        return "offline failing state-store contract";
    }

    @Override
    protected String userPrompt(LiteFlowAgentContext context) {
        return "offline";
    }
}
