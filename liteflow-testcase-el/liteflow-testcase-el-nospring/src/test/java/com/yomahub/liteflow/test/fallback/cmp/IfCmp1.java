package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.core.NodeIfComponent;

public class IfCmp1 extends NodeIfComponent {

    @Override
    public boolean processIf() throws Exception {
        return true;
    }
}
