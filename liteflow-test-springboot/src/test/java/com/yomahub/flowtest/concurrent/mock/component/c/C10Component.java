package com.yomahub.flowtest.concurrent.mock.component.c;

import com.yomahub.flowtest.concurrent.ConcurrentCase;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * desc :
 * name : C10
 *
 * @author : xujia
 * date : 2021/3/25
 * @since : 1.8
 */
@Component("c10")
public class C10Component extends NodeComponent {

    private static final String name = "c10";

    @Override
    public void process() throws Exception {
        Thread.sleep(1_000);
        ConcurrentCase.caseAdd((String) getSlot().getRequestData(), new ConcurrentCase.Routers(getSlotIndex(), name));
        System.out.println(String.format("[%s] component executed, index[%d].", name, getSlotIndex()));
    }
}