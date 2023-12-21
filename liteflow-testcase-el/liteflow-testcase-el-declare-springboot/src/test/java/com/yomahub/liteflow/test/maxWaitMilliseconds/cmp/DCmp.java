package com.yomahub.liteflow.test.maxWaitMilliseconds.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent("d")
public class DCmp {

    public static final String CONTENT_KEY = "testKey";

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS)
    public void process(NodeComponent bindCmp) {
        try {
            Thread.sleep(500);
            DefaultContext contextBean = bindCmp.getFirstContextBean();
            contextBean.setData(CONTENT_KEY, "value");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("DCmp executed!");
    }
}
