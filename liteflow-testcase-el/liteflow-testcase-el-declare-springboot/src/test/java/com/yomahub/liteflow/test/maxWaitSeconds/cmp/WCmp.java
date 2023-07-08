package com.yomahub.liteflow.test.maxWaitSeconds.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

@LiteflowComponent("w")
@LiteflowCmpDefine(NodeTypeEnum.WHILE)
public class WCmp {
    private int count = 0;

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_WHILE, nodeType = NodeTypeEnum.WHILE)
    public boolean processWhile(NodeComponent bindCmp) throws Exception {
        count++;
        return count <= 2;
    }
}
