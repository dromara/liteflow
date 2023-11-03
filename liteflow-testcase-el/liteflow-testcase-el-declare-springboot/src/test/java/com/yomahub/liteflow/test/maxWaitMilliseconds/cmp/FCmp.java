package com.yomahub.liteflow.test.maxWaitMilliseconds.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

@LiteflowComponent("f")
@LiteflowCmpDefine(NodeTypeEnum.IF)
public class FCmp {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_IF, nodeType = NodeTypeEnum.IF)
    public boolean processIf(NodeComponent bindCmp) throws Exception {
        return true;
    }
}
