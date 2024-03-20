package com.yomahub.liteflow.test.route.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;

@LiteflowComponent("r2")
public class R2 extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        return false;
    }
}
