package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

@LiteflowComponent("swn1")
@LiteflowCmpDefine(NodeTypeEnum.SWITCH)
public class SwitchCmp1 {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH)
    public String processSwitch(NodeComponent bindCmp) throws Exception {
        return "a";
    }
}
