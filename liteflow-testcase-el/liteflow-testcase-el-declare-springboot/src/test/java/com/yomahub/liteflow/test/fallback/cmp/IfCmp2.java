package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

@LiteflowComponent("ifn2")
@LiteflowCmpDefine(NodeTypeEnum.IF)
@FallbackCmp
public class IfCmp2 {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_IF)
    public boolean processIf(NodeComponent bindCmp) throws Exception {
        return false;
    }
}
