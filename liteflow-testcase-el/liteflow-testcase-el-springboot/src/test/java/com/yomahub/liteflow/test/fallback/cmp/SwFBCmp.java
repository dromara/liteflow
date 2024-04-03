package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;

@LiteflowComponent("fb_sw_cmp")
@FallbackCmp
public class SwFBCmp extends NodeSwitchComponent {

    @Override
    public String processSwitch() throws Exception {
        return "b";
    }
}
