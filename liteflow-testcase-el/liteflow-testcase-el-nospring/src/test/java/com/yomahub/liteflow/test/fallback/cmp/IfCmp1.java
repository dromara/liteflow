package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;

public class IfCmp1 extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() throws Exception {
        return true;
    }
}
