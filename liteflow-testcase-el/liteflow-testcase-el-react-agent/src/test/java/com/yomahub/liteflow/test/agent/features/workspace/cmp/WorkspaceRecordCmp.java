package com.yomahub.liteflow.test.agent.features.workspace.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * workspace 功能包的记录节点。
 */
@Component("workspaceRecord")
public class WorkspaceRecordCmp extends NodeComponent {
    @Override
    public void process() {
        getSlot().setOutput(getNodeId(), getSlot().getResponseData());
    }
}
