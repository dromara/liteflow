package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.core.NodeForComponent;

@FallbackCmp
public class ForFBCmp extends NodeForComponent {

    @Override
    public int processFor() throws Exception {
        return 3;
    }
}
