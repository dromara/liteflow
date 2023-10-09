package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.core.NodeComponent;

public class DCmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        throw new RuntimeException("component[d]");
    }
}
