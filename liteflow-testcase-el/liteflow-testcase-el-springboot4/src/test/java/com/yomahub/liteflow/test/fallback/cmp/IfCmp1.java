package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;

@LiteflowComponent("ifn1")
public class IfCmp1 extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() throws Exception {
        return true;
    }
}
