package com.yomahub.liteflow.test.retryTimes.cmp;


import com.yomahub.liteflow.core.NodeComponent;

public class ACmp extends NodeComponent {
    @Override
    public void process() {
        System.out.println("ACmp executed!");
    }
}
