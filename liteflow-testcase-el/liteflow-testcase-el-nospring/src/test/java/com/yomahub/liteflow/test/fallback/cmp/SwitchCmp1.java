package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;

public class SwitchCmp1 extends NodeSwitchComponent {

    @Override
    public String processSwitch() throws Exception {
        return "a";
    }
}
