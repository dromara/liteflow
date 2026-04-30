package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeForComponent;

@LiteflowComponent("fb_for_cmp")
@FallbackCmp
public class ForFBCmp extends NodeForComponent {

    @Override
    public int processFor() throws Exception {
        return 3;
    }
}
