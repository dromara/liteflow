package com.yomahub.liteflow.benchmark.cmp;

import com.yomahub.liteflow.benchmark.context.BatchMessageResultContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.benchmark.vo.QueryVO;
import com.yomahub.liteflow.core.NodeComponent;

import java.util.Random;

@LiteflowComponent(id = "channel3Query", name = "获取渠道3剩余量")
public class Channel3QueryCmp extends NodeComponent {
    @Override
    public void process() throws Exception {

        //mock下渠道3有10w条剩余量
        BatchMessageResultContext context = this.getFirstContextBean();
        context.addQueryVO(new QueryVO("channel3", 100000));
    }
}
