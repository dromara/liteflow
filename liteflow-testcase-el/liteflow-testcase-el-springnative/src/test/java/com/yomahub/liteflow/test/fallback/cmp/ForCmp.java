package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeForComponent;

@LiteflowComponent("for1")
@FallbackCmp
public class ForCmp extends NodeForComponent {

    @Override
    public int processFor() throws Exception {
        return 3;
    }
}
