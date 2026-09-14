package com.yomahub.liteflow.agent.conversation;

import com.yomahub.liteflow.agent.guard.AgentInvocationKey;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Process-local resources. Callers must hold the conversation's workspace invocation lease. */
public final class AgentConversationResourceRegistry {
    private static final ConcurrentHashMap<AgentInvocationKey, AutoCloseable> RESOURCES = new ConcurrentHashMap<>();

    private AgentConversationResourceRegistry() { }

    public static void register(AgentInvocationKey workspace, AutoCloseable resource) {
        if (RESOURCES.putIfAbsent(workspace, Objects.requireNonNull(resource, "resource")) != null) {
            throw new IllegalStateException("Conversation resource is already registered");
        }
    }

    public static void unregister(AgentInvocationKey workspace, AutoCloseable resource) {
        RESOURCES.remove(workspace, resource);
    }

    /** Also transfers ownership between Agent components that share one conversation workspace. */
    public static void release(AgentInvocationKey workspace) {
        AutoCloseable resource = RESOURCES.get(workspace);
        if (resource != null) {
            try {
                resource.close();
                RESOURCES.remove(workspace, resource);
            } catch (RuntimeException failure) {
                throw failure;
            } catch (Exception failure) {
                throw new IllegalStateException("Cannot release conversation resource", failure);
            }
        }
    }
}
