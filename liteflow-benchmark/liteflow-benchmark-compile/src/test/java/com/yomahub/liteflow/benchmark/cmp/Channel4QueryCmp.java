package com.yomahub.liteflow.benchmark.cmp;

import com.yomahub.liteflow.benchmark.context.BatchMessageResultContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.benchmark.vo.QueryVO;
import com.yomahub.liteflow.core.NodeComponent;

import java.util.Random;

@LiteflowComponent(id = "channel4Query", name = "获取渠道4剩余量")
public class Channel4QueryCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        //mock下渠道4有7w条剩余量
        BatchMessageResultContext context = this.getFirstContextBean();
        context.addQueryVO(new QueryVO("channel4", 70000));
    }
}
