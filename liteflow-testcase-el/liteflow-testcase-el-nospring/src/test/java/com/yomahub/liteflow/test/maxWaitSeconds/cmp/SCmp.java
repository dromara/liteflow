package com.yomahub.liteflow.test.maxWaitSeconds.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;

public class SCmp extends NodeSwitchComponent {

    @Override
    public String processSwitch() throws Exception {
        return "b";
    }
}
