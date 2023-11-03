package com.yomahub.liteflow.test.maxWaitMilliseconds.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

@LiteflowComponent("c")
public class CCmp {

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS)
    public void process(NodeComponent bindCmp) {
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("CCmp executed!");
    }
}
