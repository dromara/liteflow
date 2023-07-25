package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.exception.CustomStatefulException;
import org.springframework.stereotype.Component;


@Component("h")
public class HCmp {

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS)
    public void process(NodeComponent bindCmp) {
        DefaultContext context = bindCmp.getFirstContextBean();
        context.setData("threadName", Thread.currentThread().getName());
        System.out.println("HCmp executed!");
    }

}
