package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.core.NodeSwitchComponent;

@FallbackCmp
public class SwFBCmp extends NodeSwitchComponent {

    @Override
    public String processSwitch() throws Exception {
        return "b";
    }
}
