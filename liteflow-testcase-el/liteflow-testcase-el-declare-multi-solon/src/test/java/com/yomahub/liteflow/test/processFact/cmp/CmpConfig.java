package com.yomahub.liteflow.test.processFact.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowFact;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.test.processFact.context.Company;
import com.yomahub.liteflow.test.processFact.context.TestContext;
import com.yomahub.liteflow.test.processFact.context.User;

@LiteflowComponent
public class CmpConfig {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeType = NodeTypeEnum.COMMON, nodeId = "a")
    public void processA(NodeComponent bindCmp,
                        @LiteflowFact("testCxt") TestContext context,
                        @LiteflowFact("user") User user,
                        @LiteflowFact("user.company.address") String address) {
        user.setName("jack");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeType = NodeTypeEnum.COMMON, nodeId = "b")
    public void processB(NodeComponent bindCmp,
                        @LiteflowFact("user.company") Company company,
                        @LiteflowFact("data2") Integer data) {
        company.setHeadCount(20);
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeType = NodeTypeEnum.COMMON, nodeId = "c")
    public void processC(NodeComponent bindCmp,
                        @LiteflowFact("demo2Context.user") User user) {
        user.setName("rose");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeType = NodeTypeEnum.COMMON, nodeId = "d")
    public void processD(NodeComponent bindCmp,
                        @LiteflowFact("ctx.user") User user) {
        user.setName("jelly");
    }
}
