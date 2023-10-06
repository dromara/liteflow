package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.core.NodeIfComponent;

@FallbackCmp
public class IfCmp2 extends NodeIfComponent {

    @Override
    public boolean processIf() throws Exception {
        return false;
    }
}
