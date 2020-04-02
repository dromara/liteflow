package com.yomahub.flowtest.components;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("q")
public class QComponent extends NodeComponent {
    @Override
    public void process() throws Exception {
        this.getSlot().setData("p_flag",10);
        System.out.println("Qcomponent executed!");
    }
}
