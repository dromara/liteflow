package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * 通用准备节点：把链入参原样写入 chainReqData，供 Agent 的 userPrompt() 读取。
 */
@Component("preparePrompt")
public class PreparePromptCmp extends NodeComponent {

    @Override
    public void process() {
        getSlot().setChainReqData(getSlot().getChainId(), getRequestData());
    }
}
