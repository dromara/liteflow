package com.yomahub.liteflow.test.agent.feature.whenparallel;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * WHEN 并发分支 Agent B：覆写 agentKey 含 requestId，确保和分支 A 拥有独立的 Session 锁，
 * 这样才能真正并发执行而不互相串行。
 */
@Component("parallelAgentB")
public class ParallelAgentBCmp extends ReActAgentComponent {

    public static final AtomicReference<String> SEEN_AGENT_KEY = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_REPLY = new AtomicReference<>();

    public static void reset() {
        SEEN_AGENT_KEY.set(null);
        SEEN_REPLY.set(null);
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
        return "parallelAgentB__" + getSlot().getRequestId();
    }

    @Override
    protected String userPrompt() {
        SEEN_AGENT_KEY.set(ctx().getAgentKey());
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected void handleReply(Msg reply) {
        String text = reply == null ? "" : reply.getTextContent();
        SEEN_REPLY.set(text);
        ctx().getSlot().setOutput("parallelAgentB", text);
    }
}
