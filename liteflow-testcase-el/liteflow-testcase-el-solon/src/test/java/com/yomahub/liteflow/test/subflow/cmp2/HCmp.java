package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.noear.solon.annotation.Component;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowELSpringbootTest.RUN_TIME_SLOT;


@Component("h")
public class HCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        String requestData = this.getSubChainReqData();
        DefaultContext context = this.getFirstContextBean();
        context.setData("innerRequest", requestData);

        RUN_TIME_SLOT.add(this.getSlot().getRequestId());

        System.out.println("Hcomp executed!");
    }
}
