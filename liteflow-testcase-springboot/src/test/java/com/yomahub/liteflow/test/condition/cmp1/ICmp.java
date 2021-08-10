package com.yomahub.liteflow.test.condition.cmp1;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.condition.BaseConditionFlowTest;
import org.springframework.stereotype.Component;



@Component("i")
public class ICmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        BaseConditionFlowTest.RUN_TIME_SLOT.add(this.getSlot().getRequestId());
        System.out.println(BaseConditionFlowTest.RUN_TIME_SLOT.size());
        System.out.println("Icomp executed! throw Exception!");
        throw new RuntimeException("主动抛出异常");
    }
}
