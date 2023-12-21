package com.yomahub.liteflow.test.maxWaitMilliseconds.cmp;

import com.yomahub.liteflow.core.NodeComponent;

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
