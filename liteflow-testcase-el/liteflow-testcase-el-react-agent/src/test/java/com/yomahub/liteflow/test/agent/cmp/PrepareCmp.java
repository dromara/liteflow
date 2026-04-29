package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * 准备节点：把链入参原样作为 chainReqData，
 * 同时模拟一些前置工作（打日志、生成 requestId 等）。
 */
@Component("prepare")
public class PrepareCmp extends NodeComponent {

    @Override
    public void process() {
        Object req = this.getRequestData();
        // 把 request data 写到 chainReqData，让 agent 通过 ctx.getSlot().getChainReqData(...) 取到
        this.getSlot().setChainReqData(this.getSlot().getChainId(), req);
        System.out.println("[prepare] question=" + req);
    }
}
