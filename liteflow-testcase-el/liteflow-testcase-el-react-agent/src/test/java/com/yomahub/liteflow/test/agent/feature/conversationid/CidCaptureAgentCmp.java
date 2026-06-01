package com.yomahub.liteflow.test.agent.feature.conversationid;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 在 userPrompt 中捕获 ctx().getConversationId()，用于验证 conversationId 的多条解析路径。
 */
@Component("cidCaptureAgent")
public class CidCaptureAgentCmp extends ReActAgentComponent {

    public static final AtomicReference<String> SEEN_CONVERSATION_ID = new AtomicReference<>();

    public static void reset() {
        SEEN_CONVERSATION_ID.set(null);
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
    protected String userPrompt() {
        SEEN_CONVERSATION_ID.set(ctx().getConversationId());
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

    @Override
    protected boolean enableWorkspaceFileTools() {
        return false;
    }

    @Override
    protected boolean enableReActLogging() {
        return false;
    }
}
