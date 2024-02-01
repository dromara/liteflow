package com.yomahub.liteflow.test.maxWaitSeconds.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent("e")
public class ECmp extends NodeComponent {
    @Override
    public void process() throws Exception{
        DefaultContext context = this.getFirstContextBean();
        for (int i = 0; i < 10; i++) {
            String str = context.getData("test");
            System.out.println(str);
            Thread.sleep(1000);
        }

        System.out.println("ECmp executed!");
    }
}
