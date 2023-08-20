package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.core.NodeWhileComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("z")
public class ZCmp extends NodeWhileComponent {

    @Override
    public boolean processWhile() throws Exception {
        DefaultContext context = this.getFirstContextBean();
        String key = "test";
        if (context.hasData(key)) {
            int count = context.getData("test");
            return count < 5;
        } else {
            return true;
        }
    }
}
