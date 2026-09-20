package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.guard.AgentInvocationKey;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.yomahub.liteflow.agent.harness.sandbox.AgentSandboxStatus.State;

/**
 * Observes sandboxes owned by SessionSandboxRegistry in this JVM, across Agent components.
 * Supports SESSION_IDLE and shared-storage PER_CALL. No session-store access, workspace lease,
 * runtime initialization or sandbox creation is performed by a query.
 * Applications must authorize the user/conversation before exposing this service over HTTP.
 * NOT_ALLOCATED means no local registration, including before first use and after eviction;
 * it makes no claim about containers owned by another application instance.
 */
public final class AgentSandboxStatusService {
    private static final ConcurrentHashMap<AgentInvocationKey, Source> SOURCES = new ConcurrentHashMap<>();
    private final String namespace;
    private final Function<String, DockerContainerInspector.Inspection> inspector;

    public AgentSandboxStatusService(String namespace) {
        this(namespace, new DockerContainerInspector()::inspect);
    }

    AgentSandboxStatusService(String namespace, Function<String, DockerContainerInspector.Inspection> inspector) {
        if (namespace == null || namespace.isBlank()) throw new IllegalArgumentException("namespace must not be blank");
        this.namespace = namespace;
        this.inspector = Objects.requireNonNull(inspector, "inspector");
    }

    public AgentSandboxStatus getStatus(String conversationId) {
        AgentInvocationKey key = AgentInvocationKey.workspace(namespace, conversationId);
        Source source = SOURCES.get(key);
        if (source == null) return unallocated(conversationId);
        Snapshot snapshot = source.snapshot();
        String id = snapshot.containerId();
        var observation = id == null || id.isBlank()
                ? new DockerContainerInspector.Inspection(snapshot.busy() ? State.STARTING : State.UNKNOWN,
                        null, null, null, null)
                : inspector.apply(id);
        // Docker IO must not hold the registry monitor or block an active invocation. Discard
        // observations if eviction, handover or container recreation happened during the query.
        if (SOURCES.get(key) != source) {
            if (!SOURCES.containsKey(key)) return unallocated(conversationId);
            return new AgentSandboxStatus(conversationId, "PROCESS_LOCAL", State.UNKNOWN,
                    null, null, null, false, null, System.currentTimeMillis(), "Sandbox owner changed; retry");
        }
        Snapshot latest = source.snapshot();
        if (!Objects.equals(id, latest.containerId())) {
            observation = DockerContainerInspector.Inspection.unknown("Container changed during status query; retry");
        }
        return new AgentSandboxStatus(conversationId, "PROCESS_LOCAL", observation.state(),
                latest.containerId(), observation.name() == null ? latest.containerName() : observation.name(),
                observation.image() == null ? latest.image() : observation.image(), latest.busy(),
                latest.lastActiveAt(), System.currentTimeMillis(), observation.message());
    }

    private static AgentSandboxStatus unallocated(String conversationId) {
        return new AgentSandboxStatus(conversationId, "PROCESS_LOCAL", State.NOT_ALLOCATED,
                null, null, null, false, null, System.currentTimeMillis(), null);
    }

    static void register(AgentInvocationKey key, Source source) { SOURCES.put(key, source); }
    static void unregister(AgentInvocationKey key, Source source) { SOURCES.remove(key, source); }

    interface Source { Snapshot snapshot(); }
    record Snapshot(String containerId, String containerName, String image, boolean busy, Long lastActiveAt) { }
}
