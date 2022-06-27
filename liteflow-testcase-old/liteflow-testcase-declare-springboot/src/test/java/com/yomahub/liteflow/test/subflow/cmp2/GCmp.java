package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowSpringbootTest.RUN_TIME_SLOT;


@Component("g")
@LiteflowCmpDefine
public class GCmp{

    @Autowired
    private FlowExecutor flowExecutor;

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS)
    public void process(NodeComponent bindCmp) throws Exception {

        RUN_TIME_SLOT.add(bindCmp.getSlot().getRequestId());

        System.out.println("Gcmp executed!");

        flowExecutor.invoke("chain4", "it's implicit subflow.", bindCmp.getSlotIndex());
    }
}
