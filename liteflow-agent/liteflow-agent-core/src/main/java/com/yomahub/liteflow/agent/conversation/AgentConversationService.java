package com.yomahub.liteflow.agent.conversation;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuard;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuardResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.agent.state.DefaultAgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.message.Msg;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Storage-neutral conversation management. No model, Agent runtime or sandbox is constructed.
 *
 * <p>Uses the configured invocation guard, including its CONVERSATION scope for short metadata
 * operations. Use the same distributed guard as Agent execution when running multiple replicas.
 * Each message is a separate state entry; message paging reads only the requested entries.
 * List paging scans metadata (the underlying AgentStateStore has no indexed paging contract).
 *
 * <p>Deletion is retryable. A small tombstone prevents late writers or a reused ID from bringing
 * back a deleted conversation. Registered Agent states are removed after their calls finish.
 * Workspace files, sandbox snapshots and long-term memories are deliberately retained.
 */
public final class AgentConversationService implements AutoCloseable {

    private static final String METADATA = "conversation";
    private final String namespace;
    private final String prefix;
    private final InvocationIdentityResolver identities;
    private final AgentStateStore store;
    private final ResolvedAgentStateStore ownedStore;
    private final AgentInvocationGuard guard;
    private final Duration timeout;
    private final List<AgentStateAddressProvider> addressProviders;
    private final ReentrantReadWriteLock lifecycle = new ReentrantReadWriteLock();
    private boolean closed;

    /** Opens an owned store using liteflow.agent.state-store.*. Close this service on shutdown. */
    public static AgentConversationService open(AgentConfig config) {
        validateConfig(config);
        AgentInvocationGuard guard = new AgentInvocationGuardResolver().resolve(config);
        ResolvedAgentStateStore resolved = new DefaultAgentStateStoreResolver().resolve(config.getStateStore());
        try {
            return new AgentConversationService(config, resolved.store(), resolved, guard);
        } catch (RuntimeException | Error failure) {
            try {
                resolved.close();
            } catch (RuntimeException | Error closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }

    /** Borrows an application-owned store, including stores supplied by custom resolvers. */
    public AgentConversationService(AgentConfig config, AgentStateStore store) {
        this(config, store, null, new AgentInvocationGuardResolver().resolve(config));
    }

    private AgentConversationService(AgentConfig config, AgentStateStore store,
                                     ResolvedAgentStateStore ownedStore, AgentInvocationGuard guard) {
        validateConfig(config);
        this.namespace = config.getRuntime().getNamespace();
        this.identities = new InvocationIdentityResolver(namespace);
        this.prefix = "lf-conversation." + identities.resolve("index", "index", "conversation").agentNamespace() + ".";
        this.store = Objects.requireNonNull(store, "store");
        this.ownedStore = ownedStore;
        this.guard = guard;
        this.timeout = Objects.requireNonNull(config.getInvocationGuard().getAcquireTimeout(), "acquireTimeout");
        this.addressProviders = ServiceLoader.load(AgentStateAddressProvider.class,
                AgentStateAddressProvider.class.getClassLoader()).stream().map(ServiceLoader.Provider::get).toList();
    }

    public AgentConversation create(String userId, String title) {
        return create(userId, UUID.randomUUID().toString(), title, true);
    }

    /**
     * Creates an explicit business ID. Set recordAgentMessages=false when the application records
     * its own user-facing inputs/results with append(), e.g. a chain containing multiple Agents.
     * Agent participation is still tracked when conversation-history-enabled=true.
     */
    public AgentConversation create(String userId, String conversationId, String title, boolean recordAgentMessages) {
        return locked(userId, conversationId, () -> {
            if (load(userId, conversationId).isPresent()) {
                throw new IllegalStateException("Conversation ID already used: " + conversationId);
            }
            StoredConversation state = fresh(conversationId, title, recordAgentMessages);
            save(userId, state);
            return state.conversation();
        });
    }

    public Optional<AgentConversation> get(String userId, String conversationId) {
        return locked(userId, conversationId,
                () -> load(userId, conversationId).filter(state -> !state.deleted()).map(StoredConversation::conversation));
    }

    /** Newest activity first, with ID as a stable tie-breaker. Cursor is an offset in metadata. */
    public AgentConversationPage<AgentConversation> list(String userId, long cursor, int limit) {
        checkPage(cursor, limit);
        requireText(userId, "userId");
        return operation(() -> {
            List<AgentConversation> all = store.listSessionIds(userId).stream()
                    .filter(id -> id.startsWith(prefix))
                    .filter(id -> !id.startsWith(prefix + "deleted."))
                    // Some SQL collations compare the user prefix case-insensitively. Recheck
                    // the full address, whose hash includes the exact logical user identity.
                    .map(id -> store.get(userId, id, METADATA, StoredConversation.class)
                            .filter(state -> id.equals(slot(userId, state.conversation().id()))))
                    .flatMap(Optional::stream).filter(state -> !state.deleted())
                    .filter(state -> !store.exists(userId, tombstoneSlot(userId, state.conversation().id())))
                    .map(StoredConversation::conversation)
                    .sorted(Comparator.comparingLong(AgentConversation::updatedAt).reversed()
                            .thenComparing(AgentConversation::id)).toList();
            int from = (int) Math.min(cursor, all.size());
            int to = Math.min(from + limit, all.size());
            return new AgentConversationPage<>(all.subList(from, to), to, to < all.size());
        });
    }

    /** Cursor is the last sequence already received; zero starts at the first message. */
    public AgentConversationPage<AgentConversationMessage> messages(
            String userId, String conversationId, long cursor, int limit) {
        checkPage(cursor, limit);
        return locked(userId, conversationId, () -> {
            long count = requireConversation(userId, conversationId).conversation().messageCount();
            long start = Math.min(cursor, count);
            long end = start + Math.min(limit, count - start);
            List<AgentConversationMessage> messages = new ArrayList<>();
            for (long sequence = start + 1; sequence <= end; sequence++) {
                messages.add(store.get(userId, slot(userId, conversationId), messageKey(sequence),
                        AgentConversationMessage.class).orElseThrow(
                        () -> new IllegalStateException("Conversation message is missing: " + conversationId)));
            }
            return new AgentConversationPage<>(messages, end, end < count);
        });
    }

    public AgentConversationMessage append(String userId, String conversationId,
                                            String role, String stage, String content) {
        return append(userId, conversationId, role, stage, content, null, null);
    }

    public AgentConversationMessage append(String userId, String conversationId, String role,
                                            String stage, String content, String agentKey, String requestId) {
        requireText(role, "role");
        requireText(stage, "stage");
        Objects.requireNonNull(content, "content");
        return locked(userId, conversationId, () -> appendLocked(userId,
                requireConversation(userId, conversationId), role, stage, content, agentKey, requestId));
    }

    /** Null title preserves the title. Attributes replace the previous attribute map. */
    public AgentConversation update(String userId, String conversationId, String title, Map<String, String> attributes) {
        Map<String, String> values = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
        return locked(userId, conversationId, () -> {
            StoredConversation state = requireConversation(userId, conversationId);
            AgentConversation current = state.conversation();
            AgentConversation updated = new AgentConversation(current.id(), title == null ? current.title() : title,
                    current.createdAt(), now(), current.messageCount(), values);
            save(userId, new StoredConversation(updated, state.recordAgentMessages(), state.agents(), false));
            return updated;
        });
    }

    /**
     * Reads the persisted working context, including sessions created before the conversation API.
     * This is a snapshot, and may already have been compacted. It does not create a conversation.
     */
    public Optional<AgentState> agentState(String userId, String conversationId, String agentKey) {
        AgentInvocationIdentity identity = identities.resolve(userId, conversationId, agentKey);
        return operation(() -> {
            try (var ignored = guard.acquire(AgentInvocationKey.state(identity), timeout)) {
                Optional<StoredConversation> metadata = load(userId, conversationId);
                if (metadata.isPresent() && metadata.get().deleted()) {
                    return Optional.empty();
                }
                Set<String> addresses = addresses(identity);
                metadata.ifPresent(state -> state.agents().stream()
                        .filter(agent -> agent.agentKey().equals(agentKey)).map(AgentLocation::sessionId)
                        .forEach(addresses::add));
                AgentState found = null;
                for (String address : addresses) {
                    Optional<AgentState> candidate = store.get(userId, address, "agent_state", AgentState.class);
                    if (candidate.isPresent()) {
                        if (found != null) {
                            throw new IllegalStateException("Multiple Agent states found for agentKey: " + agentKey);
                        }
                        found = candidate.get();
                    }
                }
                // In-memory custom stores can return live objects; the public API always returns a snapshot.
                return Optional.ofNullable(found).map(state -> AgentState.fromJsonString(state.toJson()));
            }
        });
    }

    /** Associates existing state with a managed conversation, for migration or custom runtimes. */
    public void attachAgent(String userId, String conversationId, String agentKey) {
        AgentInvocationIdentity identity = identities.resolve(userId, conversationId, agentKey);
        locked(userId, conversationId, () -> {
            StoredConversation state = requireConversation(userId, conversationId);
            Set<AgentLocation> agents = new LinkedHashSet<>(state.agents());
            addresses(identity).forEach(address -> agents.add(new AgentLocation(agentKey, address)));
            save(userId, new StoredConversation(state.conversation(), state.recordAgentMessages(), List.copyOf(agents), false));
            return null;
        });
    }

    public void delete(String userId, String conversationId) {
        operation(() -> {
            StoredConversation deleting = locked(userId, conversationId, () -> {
                StoredConversation existing = load(userId, conversationId)
                        .orElseGet(() -> fresh(conversationId, "", false));
                StoredConversation marker = new StoredConversation(existing.conversation(),
                        false, existing.agents(), true);
                store.save(userId, tombstoneSlot(userId, conversationId), METADATA, marker);
                return marker;
            });
            // Never hold the metadata lock while waiting for execution: a finishing call may journal.
            try (var workspace = guard.acquire(AgentInvocationKey.workspace(namespace, userId, conversationId), timeout)) {
                for (AgentLocation agent : deleting.agents()) {
                    try (var state = guard.acquire(AgentInvocationKey.state(namespace, userId, conversationId, agent.agentKey()), timeout)) {
                        store.delete(userId, agent.sessionId());
                    }
                }
                locked(userId, conversationId, () -> {
                    // The separate tombstone survives full deletion, including unpublished messages
                    // left by a failed append/import. No per-key delete support is required.
                    store.delete(userId, slot(userId, conversationId));
                    AgentConversation empty = new AgentConversation(conversationId, "", 0, now(), 0, Map.of());
                    store.save(userId, tombstoneSlot(userId, conversationId), METADATA,
                            new StoredConversation(empty, false, List.of(), true));
                    return null;
                });
            }
            return null;
        });
    }

    /**
     * Publishes legacy display history only if the ID has never been used, including tombstones.
     * Messages are written before metadata, making retries safe after a partial failed import.
     */
    public boolean importIfAbsent(String userId, AgentConversation conversation, List<AgentConversationMessage> messages) {
        List<AgentConversationMessage> snapshot = List.copyOf(messages);
        return locked(userId, conversation.id(), () -> {
            if (load(userId, conversation.id()).isPresent()) {
                return false;
            }
            long sequence = 0;
            for (AgentConversationMessage message : snapshot) {
                AgentConversationMessage imported = new AgentConversationMessage(++sequence, message.id(),
                        message.role(), message.stage(), message.content(), message.agentKey(), message.requestId(), message.timestamp());
                store.save(userId, slot(userId, conversation.id()), messageKey(sequence), imported);
            }
            AgentConversation imported = new AgentConversation(conversation.id(), conversation.title(),
                    conversation.createdAt(), conversation.updatedAt(), sequence, conversation.attributes());
            save(userId, new StoredConversation(imported, false, List.of(), false));
            return true;
        });
    }

    /** Runtime integration; callers already hold the Agent state lease. */
    public void beginInvocation(AgentInvocationIdentity identity, String physicalSessionId, List<Msg> input, String requestId) {
        requireNamespace(identity);
        locked(identity.userId(), identity.conversationId(), () -> {
            StoredConversation state = load(identity.userId(), identity.conversationId())
                    .orElseGet(() -> fresh(identity.conversationId(), "", true));
            if (state.deleted()) {
                throw new IllegalStateException("Conversation was deleted: " + identity.conversationId());
            }
            Set<AgentLocation> agents = new LinkedHashSet<>(state.agents());
            agents.add(new AgentLocation(identity.agentKey(), physicalSessionId));
            state = new StoredConversation(state.conversation(), state.recordAgentMessages(), List.copyOf(agents), false);
            save(identity.userId(), state);
            if (state.recordAgentMessages()) {
                for (Msg message : input) {
                    appendLocked(identity.userId(), state, "user", "input", message.getTextContent(), identity.agentKey(), requestId);
                    state = requireConversation(identity.userId(), identity.conversationId());
                }
            }
            return null;
        });
    }

    /** Runtime integration; tombstones discard late results without reviving deleted conversations. */
    public void finishInvocation(AgentInvocationIdentity identity, String requestId, Msg reply, Throwable failure) {
        requireNamespace(identity);
        locked(identity.userId(), identity.conversationId(), () -> {
            Optional<StoredConversation> loaded = load(identity.userId(), identity.conversationId());
            if (loaded.isPresent() && !loaded.get().deleted() && loaded.get().recordAgentMessages()) {
                String content = failure == null ? (reply == null ? "" : reply.getTextContent())
                        : Objects.toString(failure.getMessage(), failure.getClass().getSimpleName());
                appendLocked(identity.userId(), loaded.get(), "assistant", failure == null ? "result" : "error",
                        Objects.toString(content, ""), identity.agentKey(), requestId);
            }
            return null;
        });
    }

    @Override
    public void close() {
        lifecycle.writeLock().lock();
        try {
            if (!closed) {
                closed = true;
                if (ownedStore != null) {
                    ownedStore.close();
                }
            }
        } finally {
            lifecycle.writeLock().unlock();
        }
    }

    private AgentConversationMessage appendLocked(String userId, StoredConversation state, String role,
            String stage, String content, String agentKey, String requestId) {
        AgentConversation current = state.conversation();
        long sequence = Math.addExact(current.messageCount(), 1);
        AgentConversationMessage message = new AgentConversationMessage(sequence, UUID.randomUUID().toString(),
                role, stage, content, agentKey, requestId, now());
        store.save(userId, slot(userId, current.id()), messageKey(sequence), message);
        AgentConversation updated = new AgentConversation(current.id(), current.title(), current.createdAt(),
                message.timestamp(), sequence, current.attributes());
        save(userId, new StoredConversation(updated, state.recordAgentMessages(), state.agents(), false));
        return message;
    }

    private Set<String> addresses(AgentInvocationIdentity identity) {
        Set<String> addresses = new LinkedHashSet<>();
        addresses.add(identity.storeSessionId());
        addressProviders.forEach(provider -> addresses.add(provider.sessionId(identity)));
        return addresses;
    }

    private StoredConversation fresh(String id, String title, boolean recordAgentMessages) {
        long now = now();
        return new StoredConversation(new AgentConversation(id, Objects.toString(title, ""), now, now, 0, Map.of()),
                recordAgentMessages, List.of(), false);
    }

    private StoredConversation requireConversation(String userId, String id) {
        return load(userId, id).filter(state -> !state.deleted())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + id));
    }

    private Optional<StoredConversation> load(String userId, String id) {
        Optional<StoredConversation> tombstone = store.get(userId, tombstoneSlot(userId, id), METADATA, StoredConversation.class);
        return tombstone.isPresent() ? tombstone : store.get(userId, slot(userId, id), METADATA, StoredConversation.class);
    }

    private void save(String userId, StoredConversation state) {
        store.save(userId, slot(userId, state.conversation().id()), METADATA, state);
    }

    private String slot(String userId, String id) {
        return prefix + identities.resolve(userId, id, "conversation").runtimeSessionId();
    }

    private String tombstoneSlot(String userId, String id) {
        return prefix + "deleted." + identities.resolve(userId, id, "conversation").runtimeSessionId();
    }

    private <T> T locked(String userId, String id, Supplier<T> action) {
        AgentInvocationKey key = AgentInvocationKey.conversation(namespace, userId, id);
        return operation(() -> {
            try (var ignored = guard.acquire(key, timeout)) {
                return action.get();
            }
        });
    }

    private <T> T operation(Supplier<T> action) {
        lifecycle.readLock().lock();
        try {
            if (closed) {
                throw new IllegalStateException("Conversation service is closed");
            }
            return action.get();
        } finally {
            lifecycle.readLock().unlock();
        }
    }

    private void requireNamespace(AgentInvocationIdentity identity) {
        if (!namespace.equals(identity.namespace())) {
            throw new IllegalArgumentException("Conversation namespace does not match invocation");
        }
    }

    private static String messageKey(long sequence) {
        return "message_" + sequence;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static void checkPage(long cursor, int limit) {
        if (cursor < 0 || limit < 1 || limit > 200) {
            throw new IllegalArgumentException("cursor must be non-negative and limit must be between 1 and 200");
        }
    }

    private static void validateConfig(AgentConfig config) {
        Objects.requireNonNull(config, "config").validateForExecution();
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    /** Persistence DTO; internal physical addresses are never returned by the service API. */
    public record StoredConversation(AgentConversation conversation, boolean recordAgentMessages,
                                     List<AgentLocation> agents, boolean deleted) implements State {
        public StoredConversation {
            agents = List.copyOf(agents);
        }
    }

    public record AgentLocation(String agentKey, String sessionId) {
    }
}
