package com.yomahub.liteflow.test.agent.feature.conversationid;

import com.yomahub.liteflow.test.agent.support.OfflineAgentComponent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 在 userPrompt 中捕获 context.getConversationId()，用于验证 conversationId 的多条解析路径。
 */
@Component("cidCaptureAgent")
public class CidCaptureAgentCmp extends OfflineAgentComponent {

    public static final AtomicReference<String> SEEN_CONVERSATION_ID = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_RUNTIME_SESSION = new AtomicReference<>();

    public static void reset() {
        SEEN_CONVERSATION_ID.set(null);
        SEEN_RUNTIME_SESSION.set(null);
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
    }

    @Override
    protected String userPrompt(com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
        SEEN_CONVERSATION_ID.set(context.getConversationId());
        SEEN_RUNTIME_SESSION.set(context.getRuntimeSessionId());
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected int maxIterations() {
        return 3;
    }

    @Override
    protected boolean enableShellTool() {
        return false;
    }
}
