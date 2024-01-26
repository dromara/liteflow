package com.yomahub.liteflow.test.retry.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("a")
public class ACmp extends NodeComponent {
    @Override
    public void process() {
        System.out.println("ACmp executed!");
    }
}
