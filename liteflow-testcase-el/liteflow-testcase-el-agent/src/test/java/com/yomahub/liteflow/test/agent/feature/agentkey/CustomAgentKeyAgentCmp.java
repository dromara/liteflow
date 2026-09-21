package com.yomahub.liteflow.test.agent.feature.agentkey;

import com.yomahub.liteflow.test.agent.support.OfflineAgentComponent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 覆写 agentKey()，让同一 conversationId 下不同请求拥有独立 Session。
 */
@Component("customAgentKeyAgent")
public class CustomAgentKeyAgentCmp extends OfflineAgentComponent {

    public static volatile String overriddenKey = "default-key";
    public static final AtomicReference<String> SEEN_AGENT_KEY = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_CID = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_RUNTIME_SESSION = new AtomicReference<>();

    public static void reset() {
        overriddenKey = "default-key";
        SEEN_AGENT_KEY.set(null);
        SEEN_CID.set(null);
        SEEN_RUNTIME_SESSION.set(null);
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
    }

    @Override
    protected int maxIterations() {
        return 3;
    }

    @Override
    protected boolean enableShellTool() {
        return false;
    }

    @Override
    protected String agentKey() {
        return overriddenKey;
    }

    @Override
    protected String userPrompt(com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
        SEEN_AGENT_KEY.set(context.getAgentKey());
        SEEN_CID.set(context.getConversationId());
        SEEN_RUNTIME_SESSION.set(context.getRuntimeSessionId());
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }
}
