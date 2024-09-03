package com.yomahub.liteflow.test.abstractChain.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

@LiteflowComponent
public class CmpConfig {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
    public void processA(NodeComponent bindCmp) {
        System.out.println("ACmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
    public void processB(NodeComponent bindCmp) {
        System.out.println("BCmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_BOOLEAN, nodeId = "c", nodeType = NodeTypeEnum.BOOLEAN)
    public boolean processIfC(NodeComponent bindCmp) throws Exception{
        return true;
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
    public void processD(NodeComponent bindCmp) {
        System.out.println("DCmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "e")
    public void processE(NodeComponent bindCmp) {
        System.out.println("ECmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "f", nodeType = NodeTypeEnum.SWITCH)
    public String processF(NodeComponent bindCmp) {
        return "j";
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "j")
    public void processJ(NodeComponent bindCmp) {
        System.out.println("JCmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "k")
    public void processK(NodeComponent bindCmp) {
        System.out.println("KCmp executed!");
    }
}
