package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBreakComponent;

@LiteflowComponent("bn1")
@FallbackCmp
public class BreakCmp extends NodeBreakComponent {
    
    @Override
    public boolean processBreak() throws Exception {
        return true;
    }
}
