package com.yomahub.liteflow.test.agent.feature.memorypersistence;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.springframework.stereotype.Component;

/**
 * 持久化模式测试用 Agent：固定 conversationId，真实模型调用，
 * 不关心探针，只验证不同 memory 持久化模式下链路都能正常完成。
 */
@Component("memoryAgent")
public class MemoryAgentCmp extends ReActAgentComponent {

    public static final String FIXED_CONVERSATION_ID = "memorypersist-conversation";

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

    @Override
    protected String resolveConversationId() {
        return FIXED_CONVERSATION_ID;
    }
}
