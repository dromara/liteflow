package com.yomahub.liteflow.test.maxWaitSeconds.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeIfComponent;

@LiteflowComponent("f")
public class FCmp extends NodeIfComponent {
    @Override
    public boolean processIf() throws Exception {
        return true;
    }
}
