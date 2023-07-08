package com.yomahub.liteflow.test.maxWaitSeconds.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeWhileComponent;

@LiteflowComponent("w")
public class WCmp extends NodeWhileComponent {
    private int count = 0;
    @Override
    public boolean processWhile() throws Exception {
        count++;
        return count <= 2;
    }
}
