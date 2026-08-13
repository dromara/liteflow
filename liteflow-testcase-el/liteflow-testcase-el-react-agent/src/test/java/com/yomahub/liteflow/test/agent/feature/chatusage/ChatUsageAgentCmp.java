package com.yomahub.liteflow.test.agent.feature.chatusage;

import com.yomahub.liteflow.test.agent.support.OfflineReActAgentComponent;
import com.yomahub.liteflow.test.agent.support.ScriptedChatModel;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 验证 guide §3 中 {@code context.getChatUsage()}：本次 process() 累计的 token 用量，
 * 在 handleReply（本轮 reasoning 结束后）可读。
 */
@Component("chatUsageAgent")
public class ChatUsageAgentCmp extends OfflineReActAgentComponent {

    public static final AtomicReference<ChatUsage> CAPTURED = new AtomicReference<>();
    public static final AtomicBoolean GET_USAGE_CALLED = new AtomicBoolean();

    public static void reset() {
        CAPTURED.set(null);
        GET_USAGE_CALLED.set(false);
    }

    @Override
    protected Model buildModel() {
        ChatUsage usage = ChatUsage.builder().inputTokens(7).outputTokens(3).build();
        return ScriptedChatModel.builder()
                .reply("deterministic offline reply", usage)
                .build();
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow ReAct Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
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
        // getChatUsage() 只能在 process() 生命周期内调用，handleReply 是合法时机。
        GET_USAGE_CALLED.set(true);
        CAPTURED.set(context.getChatUsage());
        super.handleReply(reply, context);
    }
}
