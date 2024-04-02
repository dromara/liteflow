package com.yomahub.liteflow.test.exception.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;

@LiteflowComponent("r1")
public class R1 extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        int testInt = this.getRequestData();
        return testInt >= 10 && testInt <= 20;
    }
}
