package com.yomahub.liteflow.test.agent.features.conversation;

import java.util.concurrent.atomic.AtomicReference;

/**
 * conversation 功能包的测试观测点。
 */
public final class ConversationFeatureProbe {
    public static final AtomicReference<String> AGENT_A_CONVERSATION_ID = new AtomicReference<>();
    public static final AtomicReference<String> AGENT_B_CONVERSATION_ID = new AtomicReference<>();
    public static final AtomicReference<String> AGENT_A_KEY = new AtomicReference<>();
    public static final AtomicReference<String> AGENT_B_KEY = new AtomicReference<>();
    public static final AtomicReference<String> AGENT_A_WORKSPACE = new AtomicReference<>();
    public static final AtomicReference<String> AGENT_B_WORKSPACE = new AtomicReference<>();

    private ConversationFeatureProbe() {
    }

    public static void reset() {
        AGENT_A_CONVERSATION_ID.set(null);
        AGENT_B_CONVERSATION_ID.set(null);
        AGENT_A_KEY.set(null);
        AGENT_B_KEY.set(null);
        AGENT_A_WORKSPACE.set(null);
        AGENT_B_WORKSPACE.set(null);
    }
}
