package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * 后处理节点：从 slot 取出 agent 写入的 responseData 打印出来，
 * 并把回复字符串塞回 slot.metaDataMap 以便测试断言（也可走 ContextBean）。
 */
@Component("recordReply")
public class RecordReplyCmp extends NodeComponent {

    public static final String NODE_ID = "recordReply";

    @Override
    public void process() {
        Object reply = this.getSlot().getResponseData();
        System.out.println("[recordReply] reply=" + reply);
        // 把 reply 作为本节点 output 存入 slot，测试可通过 slot.getOutput(NODE_ID) 取出
        if (reply != null) {
            this.getSlot().setOutput(NODE_ID, reply);
        }
    }
}

