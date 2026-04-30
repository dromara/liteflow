package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;


@Component("j")
public class JCmp extends NodeComponent{

    @Override
    public void process() throws Exception {
        DefaultContext context = this.getFirstContextBean();
        Integer loopObj = this.getCurrLoopObj();
        context.setData(loopObj.toString(), loopObj);
        Thread.sleep(100L);
    }

}
