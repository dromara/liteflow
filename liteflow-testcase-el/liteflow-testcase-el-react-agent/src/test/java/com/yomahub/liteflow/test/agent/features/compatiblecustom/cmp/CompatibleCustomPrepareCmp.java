package com.yomahub.liteflow.test.agent.features.compatiblecustom.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * compatible-custom 功能包的准备节点。
 *
 * <p>把 LiteFlow 入参写入 chainReqData，供 Agent 组件的 userPrompt() 读取。
 */
@Component("compatibleCustomPrepare")
public class CompatibleCustomPrepareCmp extends NodeComponent {
    @Override
    public void process() {
        getSlot().setChainReqData(getSlot().getChainId(), getRequestData());
    }
}
