package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.annotation.LiteflowCondCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.NodeCondComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;


@Component("e")
@LiteflowCondCmpDefine
public class ECmp{

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS_COND)
    public String processCond(NodeComponent bindCmp) throws Exception {
        System.out.println("Ecomp executed!");
        return "g";
    }
}
