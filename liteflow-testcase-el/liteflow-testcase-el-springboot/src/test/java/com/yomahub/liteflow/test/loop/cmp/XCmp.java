package com.yomahub.liteflow.test.loop.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeForComponent;

@LiteflowComponent("x")
public class XCmp extends NodeForComponent {
    @Override
    public int processFor() throws Exception {
        return 3;
    }
}
