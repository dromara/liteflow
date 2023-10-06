package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.core.NodeWhileComponent;

@FallbackCmp
public class WhileCmp2 extends NodeWhileComponent {
    
    @Override
    public boolean processWhile() throws Exception {
        return false;
    }
}
