package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowELSpringbootTest.RUN_TIME_SLOT;


@Component("m")
public class MCmp extends NodeComponent {
    @Override
    public void process() throws Exception {

        RUN_TIME_SLOT.add(this.getSlot().getRequestId());

        System.out.println("Mcomp executed!");
    }
}
