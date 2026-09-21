package com.yomahub.liteflow.test.agent.feature.handlereply;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * 通用记录节点：把 Agent 写入 slot 的 responseData 复制到本节点 output。
 * 本场景用于验证：自定义 handleReply 没写 responseData 时，下游节点拿不到回复。
 */
@Component("recordReply")
public class RecordReplyCmp extends NodeComponent {

    public static final String NODE_ID = "recordReply";

    @Override
    public void process() {
        Object reply = getSlot().getResponseData();
        if (reply != null) {
            getSlot().setOutput(NODE_ID, reply);
        }
    }
}
