package com.yomahub.liteflow.test.agent.features.shell.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * shell 功能包的记录节点。
 */
@Component("shellRecord")
public class ShellRecordCmp extends NodeComponent {
    @Override
    public void process() {
        getSlot().setOutput(getNodeId(), getSlot().getResponseData());
    }
}
