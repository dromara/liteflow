package com.yomahub.liteflow.benchmark.cmp;

import com.yomahub.liteflow.benchmark.context.BatchMessageResultContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.benchmark.vo.QueryVO;
import com.yomahub.liteflow.core.NodeComponent;

import java.util.Random;

@LiteflowComponent(id = "channel1Query", name = "获取渠道1剩余量")
public class Channel1QueryCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        //mock下渠道1有2w条剩余量
        BatchMessageResultContext context = this.getFirstContextBean();
        context.addQueryVO(new QueryVO("channel1", 20000));
    }
}
