package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

@LiteflowComponent("for1")
@LiteflowCmpDefine(NodeTypeEnum.FOR)
@FallbackCmp
public class ForCmp {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_FOR)
    public int processFor(NodeComponent bindCmp) throws Exception {
        return 3;
    }
}
