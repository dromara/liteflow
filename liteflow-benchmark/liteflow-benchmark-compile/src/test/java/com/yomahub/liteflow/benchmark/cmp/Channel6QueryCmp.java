package com.yomahub.liteflow.benchmark.cmp;

import com.yomahub.liteflow.benchmark.context.BatchMessageResultContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.benchmark.vo.QueryVO;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(id = "channel6Query", name = "获取渠道6剩余量")
public class Channel6QueryCmp extends NodeComponent {
    @Override
    public void process() throws Exception {

        //mock下渠道6有3w条剩余量
        BatchMessageResultContext context = this.getFirstContextBean();
        context.addQueryVO(new QueryVO("channel6", 30000));
    }
}
