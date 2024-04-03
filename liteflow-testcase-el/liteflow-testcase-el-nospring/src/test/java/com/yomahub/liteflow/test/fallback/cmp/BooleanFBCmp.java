package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.core.NodeBooleanComponent;

@FallbackCmp
public class BooleanFBCmp extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() throws Exception {
        return false;
    }
}
