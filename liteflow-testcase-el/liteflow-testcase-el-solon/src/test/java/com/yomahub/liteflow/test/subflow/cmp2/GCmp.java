package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowELSpringbootTest.RUN_TIME_SLOT;


@Component("g")
public class GCmp extends NodeComponent {

    @Inject
    private FlowExecutor flowExecutor;

    @Override
    public void process() throws Exception {

        RUN_TIME_SLOT.add(this.getSlot().getRequestId());

        System.out.println("Gcmp executed!");



        this.invoke("chain4", "it's implicit subflow.");
    }
}
