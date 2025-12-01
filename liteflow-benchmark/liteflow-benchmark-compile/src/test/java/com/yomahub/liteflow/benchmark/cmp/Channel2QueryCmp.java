package com.yomahub.liteflow.benchmark.cmp;

import com.yomahub.liteflow.benchmark.context.BatchMessageResultContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.benchmark.vo.QueryVO;
import com.yomahub.liteflow.core.NodeComponent;

import java.util.Random;

@LiteflowComponent(id = "channel2Query", name = "获取渠道2剩余量")
public class Channel2QueryCmp extends NodeComponent {
    @Override
    public void process() throws Exception {

        //mock下渠道2有1w条剩余量
        BatchMessageResultContext context = this.getFirstContextBean();
        context.addQueryVO(new QueryVO("channel2", 10000));
    }
}
