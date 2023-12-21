package com.yomahub.liteflow.test.maxWaitMilliseconds.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("c")
public class CCmp extends NodeComponent {
    @Override
    public void process() {
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("CCmp executed!");
    }
}
