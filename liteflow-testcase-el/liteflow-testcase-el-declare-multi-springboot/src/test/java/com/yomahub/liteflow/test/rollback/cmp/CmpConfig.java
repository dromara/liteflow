package com.yomahub.liteflow.test.rollback.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

@LiteflowComponent
public class CmpConfig {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
    public void processA(NodeComponent bindCmp) {
        System.out.println("ACmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "a")
    public void rollbackA(NodeComponent bindCmp) throws Exception {
        System.out.println("ACmp rollback!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
    public void processB(NodeComponent bindCmp) {
        System.out.println("BCmp executed!");
        throw new RuntimeException();
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.IS_CONTINUE_ON_ERROR, nodeId = "b")
    public boolean isContinueOnErrorB(NodeComponent bindCmp) {
        return true;
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "b")
    public void rollbackB(NodeComponent bindCmp) throws Exception {
        System.out.println("BCmp rollback!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
    public void processC(NodeComponent bindCmp) {
        System.out.println("CCmp executed!");
    }


    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
    public void processD(NodeComponent bindCmp) {
        System.out.println("DCmp executed!");
        throw new RuntimeException();
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "d")
    public void rollbackD(NodeComponent bindCmp) throws Exception {
        System.out.println("DCmp rollback!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "e")
    public void processE(NodeComponent bindCmp) {
        System.out.println("ECmp executed!");
        throw new RuntimeException();
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "e")
    public void rollbackE() throws Exception {
        System.out.println("ECmp rollback!");
    }

}
