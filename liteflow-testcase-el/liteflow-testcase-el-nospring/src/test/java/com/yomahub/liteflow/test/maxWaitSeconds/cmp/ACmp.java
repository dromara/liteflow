package com.yomahub.liteflow.test.maxWaitSeconds.cmp;

import com.yomahub.liteflow.core.NodeComponent;

public class ACmp extends NodeComponent {
    @Override
    public void process() {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("ACmp executed!");
    }
}
