package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultSlot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowSpringbootTest.RUN_TIME_SLOT;


@Component("g")
public class GCmp extends NodeComponent {

    @Autowired
    private FlowExecutor flowExecutor;

    @Override
    public void process() throws Exception {

        RUN_TIME_SLOT.add(this.getSlot().getRequestId());

        System.out.println("Gcmp executed!");

        flowExecutor.invoke("chain4", "it's implicit subflow.", DefaultSlot.class, this.getSlotIndex());
    }
}
