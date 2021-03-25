package com.yomahub.flowtest.concurrent.mock.component.p;

import com.yomahub.flowtest.concurrent.ConcurrentCase;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * desc :
 * name : P4Component
 *
 * @author : xujia
 * date : 2021/3/25
 * @since : 1.8
 */
@Component("p4")
public class P4Component extends NodeComponent {

    private static final String name = "p4";

    @Override
    public void process() throws Exception {
        ConcurrentCase.caseAdd((String) getSlot().getRequestData(), new ConcurrentCase.Routers(getSlotIndex(), name));
        System.out.println(String.format("[%s] component executed, index[%d].", name, getSlotIndex()));
    }
}
