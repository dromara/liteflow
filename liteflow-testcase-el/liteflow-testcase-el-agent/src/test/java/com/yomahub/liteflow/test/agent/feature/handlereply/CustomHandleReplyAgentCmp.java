package com.yomahub.liteflow.test.agent.feature.handlereply;

import com.yomahub.liteflow.test.agent.support.OfflineAgentComponent;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

/**
 * 演示 guide §2.5 "方式 2：覆写 handleReply 写入自定义位置"：
 * 不写 slot.responseData，而是用 nodeId 作为 key 写到 slot.output。
 */
@Component("customHandleReplyAgent")
public class CustomHandleReplyAgentCmp extends OfflineAgentComponent {

    public static final String OUTPUT_KEY = "customHandleReplyAgent";

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
    }

    @Override
    protected String userPrompt(com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
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
    protected void handleReply(Msg reply, com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
        context.getSlot().setOutput(OUTPUT_KEY, reply == null ? null : reply.getTextContent());
    }
}
