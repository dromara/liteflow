package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.core.NodeBooleanComponent;

@FallbackCmp
public class IfCmp2 extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() throws Exception {
        return false;
    }
}
