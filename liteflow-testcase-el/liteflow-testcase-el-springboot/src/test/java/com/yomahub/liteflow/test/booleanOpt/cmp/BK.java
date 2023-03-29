package com.yomahub.liteflow.test.booleanOpt.cmp;

import com.yomahub.liteflow.core.NodeBreakComponent;
import org.springframework.stereotype.Component;

@Component("bk")
public class BK extends NodeBreakComponent {
    @Override
    public boolean processBreak() throws Exception {
        int index = this.getLoopIndex();
        return index > 2;
    }
}
