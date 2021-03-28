package com.yomahub.flowtest.concurrent.mock.component.c;

import com.yomahub.flowtest.concurrent.ConcurrentCase;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * 测试mock component
 * @author justin.xu
 */
@Component("c5")
public class C5Component extends NodeComponent {

    private static final String name = "c5";

    @Override
    public void process() throws Exception {
        Thread.sleep(1_000);
        ConcurrentCase.caseAdd((String) getSlot().getRequestData(), new ConcurrentCase.Routers(getSlotIndex(), name));
        System.out.println(String.format("[%s] component executed, index[%d].", name, getSlotIndex()));
    }
}