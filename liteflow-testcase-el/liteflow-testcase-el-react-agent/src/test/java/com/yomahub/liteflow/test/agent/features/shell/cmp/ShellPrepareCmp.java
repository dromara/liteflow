package com.yomahub.liteflow.test.agent.features.shell.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * shell 功能包的准备节点。
 */
@Component("shellPrepare")
public class ShellPrepareCmp extends NodeComponent {
    @Override
    public void process() {
        getSlot().setChainReqData(getSlot().getChainId(), getRequestData());
    }
}
