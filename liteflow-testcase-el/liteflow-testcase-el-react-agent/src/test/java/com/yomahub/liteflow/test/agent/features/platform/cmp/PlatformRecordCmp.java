package com.yomahub.liteflow.test.agent.features.platform.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * platform 功能包的记录节点。
 */
@Component("platformRecord")
public class PlatformRecordCmp extends NodeComponent {
    @Override
    public void process() {
        getSlot().setOutput(getNodeId(), getSlot().getResponseData());
    }
}
