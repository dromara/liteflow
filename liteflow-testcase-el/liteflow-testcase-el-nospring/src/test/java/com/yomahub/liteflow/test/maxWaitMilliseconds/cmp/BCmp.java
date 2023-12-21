package com.yomahub.liteflow.test.maxWaitMilliseconds.cmp;

import com.yomahub.liteflow.core.NodeComponent;

public class BCmp extends NodeComponent {
    @Override
    public void process() {
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("BCmp executed!");
    }
}
