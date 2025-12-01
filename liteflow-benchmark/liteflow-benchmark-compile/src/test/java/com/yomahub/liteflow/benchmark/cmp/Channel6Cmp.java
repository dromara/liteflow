package com.yomahub.liteflow.benchmark.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.benchmark.context.BatchMessageResultContext;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(id = "channel6", name = "返回渠道6")
public class Channel6Cmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        BatchMessageResultContext context = this.getFirstContextBean();
        context.setFinalResultChannel("channel6");
    }
}
