package com.yomahub.liteflow.test.agent.features.conversation.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * conversation 功能包的记录节点。
 */
@Component("conversationRecord")
public class ConversationRecordCmp extends NodeComponent {
    @Override
    public void process() {
        getSlot().setOutput(getNodeId(), getSlot().getResponseData());
    }
}
