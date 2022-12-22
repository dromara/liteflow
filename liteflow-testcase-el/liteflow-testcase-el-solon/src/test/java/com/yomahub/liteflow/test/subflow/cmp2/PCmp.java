package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

@Component("p")
public class PCmp extends NodeComponent {

    @Inject
    private FlowExecutor flowExecutor;

    @Override
    public void process() throws Exception {
        int slotIndex = this.getSlotIndex();
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            new Thread(() -> flowExecutor.invoke2RespInAsync("c2", "it's implicit subflow " + finalI, slotIndex)).start();
        }
        Thread.sleep(1000);
    }
}
