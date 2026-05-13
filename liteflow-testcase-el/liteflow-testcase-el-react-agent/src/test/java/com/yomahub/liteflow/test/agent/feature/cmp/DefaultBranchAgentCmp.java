package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component("defaultBranchAgent")
public class DefaultBranchAgentCmp extends AbstractCompatibleCustomAgentCmp {

    public static final AtomicInteger INVOCATION_COUNT = new AtomicInteger();

    public static void reset() {
        INVOCATION_COUNT.set(0);
    }

    @Override
    protected String systemPrompt() {
        return "你是通用问答助手，请用一句话回答。";
    }

    @Override
    protected String userPrompt() {
        INVOCATION_COUNT.incrementAndGet();
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        if (reqData instanceof java.util.Map<?, ?> map) {
            Object p = map.get("prompt");
            if (p != null) return p.toString();
        }
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected void handleReply(Msg reply) {
        ctx().getSlot().setOutput("defaultBranchAgent", reply == null ? null : reply.getTextContent());
    }
}
