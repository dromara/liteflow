package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.customNodes.domain.DemoDomain;

import javax.annotation.Resource;

@LiteflowComponent("b")
public class BCmp extends NodeComponent {

    @Override
    public void process() {
        System.out.println("BCmp executed!");
    }

}
