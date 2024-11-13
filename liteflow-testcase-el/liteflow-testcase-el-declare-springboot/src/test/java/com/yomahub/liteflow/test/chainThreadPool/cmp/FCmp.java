package com.yomahub.liteflow.test.chainThreadPool.cmp;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("f")
public class FCmp {

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS)
    public void process(NodeComponent bindCmp) {
        DefaultContext context = bindCmp.getFirstContextBean();
        context.setData("threadName", Thread.currentThread().getName());
        System.out.println("FCmp executed!");
    }

}
