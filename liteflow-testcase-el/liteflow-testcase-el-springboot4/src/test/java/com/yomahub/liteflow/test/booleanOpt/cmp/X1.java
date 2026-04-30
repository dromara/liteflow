package com.yomahub.liteflow.test.booleanOpt.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;

@Component("x1")
public class X1 extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        return true;
    }
}
