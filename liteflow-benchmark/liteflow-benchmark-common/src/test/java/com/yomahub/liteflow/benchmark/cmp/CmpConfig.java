package com.yomahub.liteflow.benchmark.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowFact;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

@LiteflowComponent
public class CmpConfig {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
    public void processA(NodeComponent bindCmp, @LiteflowFact("name") String name) {
        System.out.println(name);
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
    public void processB(NodeComponent bindCmp, @LiteflowFact("age") int age) {
        System.out.println(age);
    }
}
