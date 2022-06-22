package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.annotation.LiteflowSwitchCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;


@Component("j")
@LiteflowSwitchCmpDefine
public class JCmp{

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS_COND)
    public String processCond(NodeComponent bindCmp) throws Exception {
        System.out.println("Jcomp executed!");
        return "chain3";
    }
}
