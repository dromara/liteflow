package com.yomahub.liteflow.test.agent.features.workspace.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * workspace 功能包的准备节点。
 */
@Component("workspacePrepare")
public class WorkspacePrepareCmp extends NodeComponent {
    @Override
    public void process() {
        getSlot().setChainReqData(getSlot().getChainId(), getRequestData());
    }
}
