package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("s")
public class S extends NodeComponent {
    @Override
    public void process() throws Exception {
        throw new RuntimeException("test");
    }
}
