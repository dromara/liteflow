package com.yomahub.liteflow.test.agent.feature.chatusage;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatUsage;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 验证 guide §3 中 {@code ctx().getChatUsage()}：本次 process() 累计的 token 用量，
 * 在 handleReply（本轮 reasoning 结束后）可读。
 */
@Component("chatUsageAgent")
public class ChatUsageAgentCmp extends ReActAgentComponent {

    public static final AtomicReference<ChatUsage> CAPTURED = new AtomicReference<>();
    public static final AtomicBoolean GET_USAGE_CALLED = new AtomicBoolean();

    public static void reset() {
        CAPTURED.set(null);
        GET_USAGE_CALLED.set(false);
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
    protected void handleReply(Msg reply) {
        // getChatUsage() 只能在 process() 生命周期内调用，handleReply 是合法时机。
        GET_USAGE_CALLED.set(true);
        CAPTURED.set(ctx().getChatUsage());
        super.handleReply(reply);
    }
}
