package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 覆写 agentKey()，让同一 conversationId 下不同请求拥有独立 Session。
 */
@Component("customAgentKeyAgent")
public class CustomAgentKeyAgentCmp extends AbstractCompatibleCustomAgentCmp {

    public static volatile String overriddenKey = "default-key";
    public static final AtomicReference<String> SEEN_AGENT_KEY = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_CID = new AtomicReference<>();

    public static void reset() {
        overriddenKey = "default-key";
        SEEN_AGENT_KEY.set(null);
        SEEN_CID.set(null);
    }

    @Override
    protected String agentKey() {
        return overriddenKey;
    }

    @Override
    protected String userPrompt() {
        SEEN_AGENT_KEY.set(ctx().getAgentKey());
        SEEN_CID.set(ctx().getConversationId());
        return super.userPrompt();
    }
}
