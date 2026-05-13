package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

/**
 * 演示 guide §2.5 "方式 2：覆写 handleReply 写入自定义位置"：
 * 不写 slot.responseData，而是用 nodeId 作为 key 写到 slot.output。
 */
@Component("customHandleReplyAgent")
public class CustomHandleReplyAgentCmp extends AbstractCompatibleCustomAgentCmp {

    public static final String OUTPUT_KEY = "customHandleReplyAgent";

    @Override
    protected void handleReply(Msg reply) {
        ctx().getSlot().setOutput(OUTPUT_KEY, reply == null ? null : reply.getTextContent());
    }
}
