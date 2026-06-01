package com.yomahub.liteflow.test.agent.feature.agentkey;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 覆写 agentKey()，让同一 conversationId 下不同请求拥有独立 Session。
 */
@Component("customAgentKeyAgent")
public class CustomAgentKeyAgentCmp extends ReActAgentComponent {

    public static volatile String overriddenKey = "default-key";
    public static final AtomicReference<String> SEEN_AGENT_KEY = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_CID = new AtomicReference<>();

    public static void reset() {
        overriddenKey = "default-key";
        SEEN_AGENT_KEY.set(null);
        SEEN_CID.set(null);
    }

    @Override
    protected ModelSpec<?> model() {
        return LiveTestSupport.compatibleCustomModel();
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow ReAct Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
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
    protected boolean enableWorkspaceFileTools() {
        return false;
    }

    @Override
    protected boolean enableReActLogging() {
        return false;
    }

    @Override
    protected String agentKey() {
        return overriddenKey;
    }

    @Override
    protected String userPrompt() {
        SEEN_AGENT_KEY.set(ctx().getAgentKey());
        SEEN_CID.set(ctx().getConversationId());
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }
}
