package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;

@LiteflowComponent("fb_bool_cmp")
@FallbackCmp
public class BooleanFBCmp extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() throws Exception {
        return false;
    }
}
