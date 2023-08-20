package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.core.NodeComponent;

public class FCmp extends NodeComponent{

    @Override
    public void process() {
        try {
            System.out.println("FCmp start to sleep 5s");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("FCmp executed!");
    }

}
