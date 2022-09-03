package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("r")
public class R extends NodeComponent {
    @Override
    public void process() throws Exception {
        this.invoke2Resp("chain_s","");
    }
}
