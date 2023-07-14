package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;


@Component("h")
public class HCmp extends NodeComponent{

    @Override
    public void process() {
        DefaultContext context = this.getFirstContextBean();
        context.setData("threadName", Thread.currentThread().getName());
        System.out.println("HCmp executed!");
    }

}
