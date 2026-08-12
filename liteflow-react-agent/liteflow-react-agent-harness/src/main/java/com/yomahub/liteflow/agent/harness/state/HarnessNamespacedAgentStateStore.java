package com.yomahub.liteflow.agent.harness.state;

import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Routes Harness Agent state separately from conversation-scoped sandbox resume state. */
public final class HarnessNamespacedAgentStateStore
        extends GuardedNamespacedAgentStateStore {

    private static final Set<String> AGENT_KEYS = Set.of(
            "agent_state", "memory_messages", "toolkit_activeGroups");
    private static final String SANDBOX_KEY = "_sandbox_state";
    private static final String WORKSPACE_PREFIX = "harness-workspace.";
    private static final Pattern SANDBOX_SESSION = Pattern.compile(
            "sandbox/session/(lf-[0-9a-f]{64})");

    private final String agentPrefix;
    private final ConcurrentMap<LoadFailureKey, Throwable> loadFailures =
            new ConcurrentHashMap<>();

    public HarnessNamespacedAgentStateStore(
            AgentStateStore delegate, String agentNamespace) {
        super(delegate, agentNamespace);
        this.agentPrefix = agentNamespace() + ".";
    }

    @Override
    public void save(String userId, String sessionId, String key, State value) {
        Route route = route(userId, sessionId, key);
        delegate().save(route.userId(), route.sessionId(), key, value);
    }

    @Override
    public void save(
            String userId, String sessionId, String key, List<? extends State> values) {
        Route route = route(userId, sessionId, key);
        delegate().save(route.userId(), route.sessionId(), key, values);
    }

    @Override
    public <T extends State> Optional<T> get(
            String userId, String sessionId, String key, Class<T> type) {
        Route route = route(userId, sessionId, key);
        try {
            return delegate().get(route.userId(), route.sessionId(), key, type);
        }
        catch (RuntimeException | Error failure) {
            recordLoadFailure(userId, route.runtimeSessionId(), failure);
            throw failure;
        }
    }

    @Override
    public <T extends State> List<T> getList(
            String userId, String sessionId, String key, Class<T> itemType) {
        Route route = route(userId, sessionId, key);
        try {
            return delegate().getList(route.userId(), route.sessionId(), key, itemType);
        }
        catch (RuntimeException | Error failure) {
            recordLoadFailure(userId, route.runtimeSessionId(), failure);
            throw failure;
        }
    }

    @Override
    public boolean exists(String userId, String sessionId) {
        Route route = routeSession(userId, sessionId);
        try {
            return delegate().exists(route.userId(), route.sessionId());
        }
        catch (RuntimeException | Error failure) {
            recordLoadFailure(userId, route.runtimeSessionId(), failure);
            throw failure;
        }
    }

    @Override
    public void delete(String userId, String sessionId) {
        Route route = routeSession(userId, sessionId);
        delegate().delete(route.userId(), route.sessionId());
    }

    @Override
    public void delete(String userId, String sessionId, String key) {
        Route route = route(userId, sessionId, key);
        delegate().delete(route.userId(), route.sessionId(), key);
    }

    /** Lists only ReAct/Harness Agent sessions; sandbox resume slots are infrastructure state. */
    @Override
    public Set<String> listSessionIds(String userId) {
        Set<String> physical = delegate().listSessionIds(userId);
        if (physical == null || physical.isEmpty()) {
            return Set.of();
        }
        return physical.stream()
                .filter(Objects::nonNull)
                .filter(session -> session.startsWith(agentPrefix))
                .map(session -> session.substring(agentPrefix.length()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<Throwable> takeLoadFailure(String userId, String runtimeSessionId) {
        requireLogicalSession(runtimeSessionId);
        Throwable exact = loadFailures.remove(new LoadFailureKey(userId, runtimeSessionId));
        if (exact != null) {
            return Optional.of(exact);
        }
        return Optional.ofNullable(loadFailures.remove(
                new LoadFailureKey(null, runtimeSessionId)));
    }

    @Override
    public void clearLoadFailure(String userId, String runtimeSessionId) {
        requireLogicalSession(runtimeSessionId);
        loadFailures.remove(new LoadFailureKey(userId, runtimeSessionId));
        loadFailures.remove(new LoadFailureKey(null, runtimeSessionId));
    }

    private Route route(String userId, String sessionId, String key) {
        requireKey(key);
        if (SANDBOX_KEY.equals(key)) {
            return workspaceRoute(userId, sessionId);
        }
        return agentRoute(userId, sessionId);
    }

    private Route routeSession(String userId, String sessionId) {
        requireLogicalSession(sessionId);
        if (sessionId.startsWith("sandbox/")) {
            return workspaceRoute(userId, sessionId);
        }
        return agentRoute(userId, sessionId);
    }

    private Route workspaceRoute(String userId, String sessionId) {
        if (userId != null) {
            throw new IllegalArgumentException(
                    "_sandbox_state must use the anonymous AgentStateStore user slot");
        }
        Matcher matcher = SANDBOX_SESSION.matcher(requireLogicalSession(sessionId));
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "_sandbox_state sessionId must match sandbox/session/lf-[0-9a-f]{64}");
        }
        String runtimeSessionId = matcher.group(1);
        return new Route(null, WORKSPACE_PREFIX + runtimeSessionId, runtimeSessionId);
    }

    private Route agentRoute(String userId, String sessionId) {
        String logical = requireLogicalSession(sessionId);
        if (logical.startsWith("sandbox/")) {
            throw new IllegalArgumentException(
                    "Agent state keys must not use a sandbox/* logical session");
        }
        return new Route(userId, agentPrefix + logical, logical);
    }

    private static void requireKey(String key) {
        if (SANDBOX_KEY.equals(key) || AGENT_KEYS.contains(key)) {
            return;
        }
        throw new IllegalArgumentException("Unsupported AgentStateStore key: " + key);
    }

    private static String requireLogicalSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        return sessionId;
    }

    private void recordLoadFailure(
            String userId, String runtimeSessionId, Throwable failure) {
        loadFailures.putIfAbsent(new LoadFailureKey(userId, runtimeSessionId), failure);
    }

    private record Route(String userId, String sessionId, String runtimeSessionId) {
    }

    private record LoadFailureKey(String userId, String runtimeSessionId) {
    }
}
