package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeWhileComponent;

@LiteflowComponent("wn2")
@FallbackCmp
public class WhileCmp2 extends NodeWhileComponent {
    
    @Override
    public boolean processWhile() throws Exception {
        return false;
    }
}
