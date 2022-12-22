package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.noear.solon.annotation.Component;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowELSpringbootTest.RUN_TIME_SLOT;


@Component("f")
public class FCmp extends NodeComponent {
    @Override
    public void process() throws Exception {

        RUN_TIME_SLOT.add(this.getSlot().getRequestId());

        DefaultContext context = this.getFirstContextBean();
        context.setData("innerRequestData", "inner request");

        System.out.println("Fcomp executed!");
    }
}
