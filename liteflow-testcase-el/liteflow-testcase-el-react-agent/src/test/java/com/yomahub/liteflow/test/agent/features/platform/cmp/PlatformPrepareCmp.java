package com.yomahub.liteflow.test.agent.features.platform.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * platform 功能包的准备节点。
 */
@Component("platformPrepare")
public class PlatformPrepareCmp extends NodeComponent {
    @Override
    public void process() {
        getSlot().setChainReqData(getSlot().getChainId(), getRequestData());
    }
}
