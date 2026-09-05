package com.yomahub.liteflow.agent.state;

import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.property.agent.AgentConfig;

import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Non-owning state-store decorator that isolates one Agent runtime by its safe namespace. */
public class GuardedNamespacedAgentStateStore implements AgentStateStore, AutoCloseable {

    private static final Pattern SAFE_HASH_ID = Pattern.compile("lf-[0-9a-f]{64}");

    private final AgentStateStore delegate;
    private final String agentNamespace;
    private final String sessionPrefix;
    private final ConcurrentMap<LoadFailureKey, Throwable> loadFailures =
            new ConcurrentHashMap<>();

    public GuardedNamespacedAgentStateStore(
            AgentStateStore delegate, String agentNamespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.agentNamespace = requireSafeHashId(agentNamespace, "agentNamespace");
        this.sessionPrefix = this.agentNamespace + ".";
    }

    public String agentNamespace() {
        return agentNamespace;
    }

    /** Runtime integration without resolving another connection or taking ownership of the store. */
    public final AgentConversationService conversationService(AgentConfig config) {
        return new AgentConversationService(config, delegate);
    }

    /** Physical address of the primary Agent state; provider subclasses override their routing. */
    public String agentStateSessionId(String runtimeSessionId) {
        return namespace(runtimeSessionId);
    }

    /** Provider extension seam for routing explicit state classes without owning the delegate. */
    protected final AgentStateStore delegate() {
        return delegate;
    }

    @Override
    public void save(String userId, String sessionId, String key, State value) {
        delegate.save(userId, namespace(sessionId), key, value);
    }

    @Override
    public void save(
            String userId, String sessionId, String key, List<? extends State> values) {
        delegate.save(userId, namespace(sessionId), key, values);
    }

    @Override
    public <T extends State> Optional<T> get(
            String userId, String sessionId, String key, Class<T> type) {
        String logicalSessionId = requireSafeHashId(sessionId, "sessionId");
        try {
            return delegate.get(userId, sessionPrefix + logicalSessionId, key, type);
        } catch (RuntimeException | Error failure) {
            recordLoadFailure(userId, logicalSessionId, failure);
            throw failure;
        }
    }

    @Override
    public <T extends State> List<T> getList(
            String userId, String sessionId, String key, Class<T> itemType) {
        String logicalSessionId = requireSafeHashId(sessionId, "sessionId");
        try {
            return delegate.getList(userId, sessionPrefix + logicalSessionId, key, itemType);
        } catch (RuntimeException | Error failure) {
            recordLoadFailure(userId, logicalSessionId, failure);
            throw failure;
        }
    }

    @Override
    public boolean exists(String userId, String sessionId) {
        String logicalSessionId = requireSafeHashId(sessionId, "sessionId");
        try {
            return delegate.exists(userId, sessionPrefix + logicalSessionId);
        } catch (RuntimeException | Error failure) {
            recordLoadFailure(userId, logicalSessionId, failure);
            throw failure;
        }
    }

    @Override
    public void delete(String userId, String sessionId) {
        delegate.delete(userId, namespace(sessionId));
    }

    @Override
    public void delete(String userId, String sessionId, String key) {
        delegate.delete(userId, namespace(sessionId), key);
    }

    @Override
    public Set<String> listSessionIds(String userId) {
        Set<String> sessionIds = delegate.listSessionIds(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Set.of();
        }
        return sessionIds.stream()
                .filter(Objects::nonNull)
                .filter(sessionId -> sessionId.startsWith(sessionPrefix))
                .map(sessionId -> sessionId.substring(sessionPrefix.length()))
                .filter(sessionId -> SAFE_HASH_ID.matcher(sessionId).matches())
                .collect(Collectors.toUnmodifiableSet());
    }

    public Optional<Throwable> takeLoadFailure(String userId, String runtimeSessionId) {
        String logicalSessionId = requireSafeHashId(runtimeSessionId, "runtimeSessionId");
        return Optional.ofNullable(loadFailures.remove(new LoadFailureKey(userId, logicalSessionId)));
    }

    public void clearLoadFailure(String userId, String runtimeSessionId) {
        String logicalSessionId = requireSafeHashId(runtimeSessionId, "runtimeSessionId");
        loadFailures.remove(new LoadFailureKey(userId, logicalSessionId));
    }

    /** This decorator never owns or closes its delegate. */
    @Override
    public void close() {
    }

    private String namespace(String sessionId) {
        return sessionPrefix + requireSafeHashId(sessionId, "sessionId");
    }

    private void recordLoadFailure(String userId, String sessionId, Throwable failure) {
        loadFailures.putIfAbsent(new LoadFailureKey(userId, sessionId), failure);
    }

    private static String requireSafeHashId(String value, String name) {
        if (value == null || !SAFE_HASH_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must match lf-[0-9a-f]{64}");
        }
        return value;
    }

    private record LoadFailureKey(String userId, String runtimeSessionId) {
    }
}
