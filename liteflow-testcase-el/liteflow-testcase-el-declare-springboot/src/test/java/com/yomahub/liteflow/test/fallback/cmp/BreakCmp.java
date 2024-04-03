package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

@LiteflowComponent("bn1")
@LiteflowCmpDefine(NodeTypeEnum.BOOLEAN)
@FallbackCmp
public class BreakCmp {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_BOOLEAN)
    public boolean processBreak(NodeComponent bindCmp) throws Exception {
        return true;
    }
}
