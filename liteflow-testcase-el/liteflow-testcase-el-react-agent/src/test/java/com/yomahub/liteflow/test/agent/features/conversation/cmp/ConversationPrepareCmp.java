package com.yomahub.liteflow.test.agent.features.conversation.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * 保留原始请求 Map，使默认 resolveConversationId() 能读取约定的 conversationId 字段。
 */
@Component("conversationPrepare")
public class ConversationPrepareCmp extends NodeComponent {
    @Override
    public void process() {
        getSlot().setChainReqData(getSlot().getChainId(), getRequestData());
    }
}
