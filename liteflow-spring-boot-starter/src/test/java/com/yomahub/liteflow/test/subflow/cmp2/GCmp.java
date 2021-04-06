package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowTest.RUN_TIME_SLOT;


@Component("g")
public class GCmp extends NodeComponent {

    @Resource
    private FlowExecutor flowExecutor;

    @Override
    public void process() throws Exception {

        RUN_TIME_SLOT.add(this.getSlotIndex());

        System.out.println("Gcomp executed!");

        flowExecutor.invoke("chain4", "it's implicit subflow.", DefaultSlot.class, this.getSlotIndex());
    }
}
