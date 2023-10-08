package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeIfComponent;

@LiteflowComponent("ifn1")
public class IfCmp1 extends NodeIfComponent {

    @Override
    public boolean processIf() throws Exception {
        return true;
    }
}
