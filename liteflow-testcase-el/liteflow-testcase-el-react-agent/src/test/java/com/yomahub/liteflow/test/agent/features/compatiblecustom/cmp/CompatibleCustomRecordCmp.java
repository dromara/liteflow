package com.yomahub.liteflow.test.agent.features.compatiblecustom.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * compatible-custom 功能包的记录节点。
 */
@Component("compatibleCustomRecord")
public class CompatibleCustomRecordCmp extends NodeComponent {
    @Override
    public void process() {
        getSlot().setOutput(getNodeId(), getSlot().getResponseData());
    }
}
