package com.yomahub.liteflow.test.booleanOpt.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;

@Component("bk")
public class BK extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        int index = this.getLoopIndex();
        return index > 2;
    }
}
