package com.yomahub.liteflow.test.retry.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("b")
public class BCmp extends NodeComponent {
    int flag = 0;

    @Override
    public void process() {
        flag ++;
        System.out.println("BCmp executed!");
        if(flag < 4) throw new RuntimeException();
        else flag = 0;
    }
}
