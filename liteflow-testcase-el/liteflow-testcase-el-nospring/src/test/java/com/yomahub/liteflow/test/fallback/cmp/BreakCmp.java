package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.core.NodeBreakComponent;

@FallbackCmp
public class BreakCmp extends NodeBreakComponent {

    @Override
    public boolean processBreak() throws Exception {
        return true;
    }
}
