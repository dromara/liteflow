package com.yomahub.liteflow.test.agent.feature.basicchain;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * 通用记录节点：把 Agent 写入 slot 的 responseData 复制到本节点 output，
 * 测试侧通过 {@code slot.getOutput(NODE_ID)} 拿到回复，验证回复流转到下游普通节点。
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
