package com.yomahub.liteflow.agent.context;

import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.model.ChatUsage;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Immutable invocation metadata plus narrowly scoped concurrent trackers. */
public final class LiteFlowAgentContext {

    public static final String SLOT_ATTACHMENT_PREFIX =
            "com.yomahub.liteflow.agent.context.LiteFlowAgentContext#";

    private final AgentInvocationIdentity identity;
    private final Slot slot;
    private final String chainId;
    private final String nodeId;
    private final String requestId;
    private final String traceId;
    private final Instant deadline;
    private final AgentOutputSpec outputSpec;
    private final String attachmentKey;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicReference<ChatUsage> chatUsage = new AtomicReference<>();
    private final Set<String> recordedUsageEvents = ConcurrentHashMap.newKeySet();
    private final Set<String> usedSkills = Collections.synchronizedSet(new LinkedHashSet<>());

    public LiteFlowAgentContext(
            AgentInvocationIdentity identity,
            Slot slot,
            String chainId,
            String nodeId,
            String requestId,
            String traceId,
            Instant deadline,
            AgentOutputSpec outputSpec,
            String attachmentKey) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.slot = Objects.requireNonNull(slot, "slot");
        this.chainId = chainId;
        this.nodeId = nodeId;
        this.requestId = requireText(requestId, "requestId");
        this.traceId = requireText(traceId, "traceId");
        this.deadline = Objects.requireNonNull(deadline, "deadline");
        this.outputSpec = Objects.requireNonNull(outputSpec, "outputSpec");
        this.attachmentKey = requireText(attachmentKey, "attachmentKey");
    }

    public AgentInvocationIdentity getIdentity() {
        return identity;
    }

    public String getNamespace() {
        return identity.namespace();
    }

    public String getUserId() {
        return identity.userId();
    }

    public String getConversationId() {
        return identity.conversationId();
    }

    public String getAgentKey() {
        return identity.agentKey();
    }

    public String getRuntimeUserId() {
        return identity.userId();
    }

    public String getRuntimeSessionId() {
        return identity.runtimeSessionId();
    }

    public String getAgentNamespace() {
        return identity.agentNamespace();
    }

    public String getStoreSessionId() {
        return identity.storeSessionId();
    }

    public Slot getSlot() {
        return slot;
    }

    public String getChainId() {
        return chainId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public AgentOutputSpec getOutputSpec() {
        return outputSpec;
    }

    public String getAttachmentKey() {
        return attachmentKey;
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cancel() {
        cancelled.set(true);
    }

    public ChatUsage getChatUsage() {
        return chatUsage.get();
    }

    public void setChatUsage(ChatUsage usage) {
        chatUsage.set(usage);
    }

    /** Adds one model-call usage event, ignoring duplicate delivery of the same event id. */
    public void recordChatUsage(String eventId, ChatUsage usage) {
        if (usage == null) {
            return;
        }
        if (eventId != null && !eventId.isBlank() && !recordedUsageEvents.add(eventId)) {
            return;
        }
        chatUsage.updateAndGet(current -> current == null
                ? usage
                : new ChatUsage(
                        current.getInputTokens() + usage.getInputTokens(),
                        current.getOutputTokens() + usage.getOutputTokens(),
                        current.getCachedTokens() + usage.getCachedTokens(),
                        current.getTime() + usage.getTime()));
    }

    public void recordUsedSkill(String skill) {
        if (skill != null && !skill.isBlank()) {
            usedSkills.add(skill);
        }
    }

    public List<String> getUsedSkills() {
        synchronized (usedSkills) {
            return List.copyOf(usedSkills);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
