package com.yomahub.liteflow.test.loop.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBreakComponent;
import com.yomahub.liteflow.core.NodeForComponent;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent("y")
public class YCmp extends NodeBreakComponent {
    @Override
    public boolean processBreak() throws Exception {
        DefaultContext context = this.getFirstContextBean();
        int count = context.getData("test");
        return count > 3;
    }
}
