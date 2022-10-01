package com.yomahub.liteflow.test.loop.cmp;

import com.yomahub.liteflow.annotation.LiteflowBreakCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeBreakComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent("y")
@LiteflowBreakCmpDefine
public class YCmp{

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS_BREAK)
    public boolean processBreak(NodeComponent bindCmp) throws Exception {
        DefaultContext context = bindCmp.getFirstContextBean();
        int count = context.getData("test");
        return count > 3;
    }
}
