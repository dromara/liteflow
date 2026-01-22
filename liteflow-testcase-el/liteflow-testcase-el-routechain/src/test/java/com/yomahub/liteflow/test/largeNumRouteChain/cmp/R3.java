package com.yomahub.liteflow.test.largeNumRouteChain.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;

@LiteflowComponent("r3")
public class R3 extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        int testInt = this.getRequestData();
        return testInt < 100;
    }
}
