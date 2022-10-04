package com.yomahub.liteflow.test.mixDefine.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.test.base.cmp.TestDomain;

import javax.annotation.Resource;

@LiteflowComponent("a")
@LiteflowCmpDefine(NodeTypeEnum.COMMON)
public class CmpConfig {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS)
    public void processA(NodeComponent bindCmp) {
        System.out.println("ACmp executed!");
    }


    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
    public void processC(NodeComponent bindCmp) {
        System.out.println("CCmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_IF, nodeId = "d", nodeType = NodeTypeEnum.IF)
    public boolean processIf(NodeComponent bindCmp) {
        return true;
    }
}


