package com.yomahub.liteflow.test.loop.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.annotation.LiteflowWhileCmpDefine;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.NodeWhileComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent("z")
@LiteflowWhileCmpDefine
public class ZCmp{

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS_WHILE)
    public boolean processWhile(NodeComponent bindCmp) throws Exception {
        DefaultContext context = bindCmp.getFirstContextBean();
        String key = "test";
        if (context.hasData(key)){
            int count = context.getData("test");
            return count < 5;
        }else{
            return true;
        }
    }
}
