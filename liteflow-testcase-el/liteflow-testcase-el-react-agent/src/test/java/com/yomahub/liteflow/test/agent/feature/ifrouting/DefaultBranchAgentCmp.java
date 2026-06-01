package com.yomahub.liteflow.test.agent.feature.ifrouting;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IF 默认分支 Agent，回复以 nodeId 为 key 写入 slot.output。
 */
@Component("defaultBranchAgent")
public class DefaultBranchAgentCmp extends ReActAgentComponent {

    public static final AtomicInteger INVOCATION_COUNT = new AtomicInteger();

    public static void reset() {
        INVOCATION_COUNT.set(0);
    }

    @Override
    protected ModelSpec<?> model() {
        return LiveTestSupport.compatibleCustomModel();
    }

    @Override
    protected String systemPrompt() {
        return "你是通用问答助手，请用一句话回答。";
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
    protected String userPrompt() {
        INVOCATION_COUNT.incrementAndGet();
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        if (reqData instanceof Map<?, ?> map) {
            Object p = map.get("prompt");
            if (p != null) {
                return p.toString();
            }
        }
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected void handleReply(Msg reply) {
        ctx().getSlot().setOutput("defaultBranchAgent", reply == null ? null : reply.getTextContent());
    }
}
