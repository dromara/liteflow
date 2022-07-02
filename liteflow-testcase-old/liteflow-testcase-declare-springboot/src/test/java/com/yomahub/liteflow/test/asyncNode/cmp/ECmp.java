package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.annotation.LiteflowSwitchCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;


@Component("e")
@LiteflowSwitchCmpDefine
public class ECmp{

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS_SWITCH)
    public String processSwitch(NodeComponent bindCmp) throws Exception {
        System.out.println("Ecomp executed!");
        return "g";
    }
}
