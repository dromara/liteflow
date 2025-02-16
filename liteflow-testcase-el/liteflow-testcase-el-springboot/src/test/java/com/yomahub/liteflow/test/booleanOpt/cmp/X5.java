package com.yomahub.liteflow.test.booleanOpt.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;

@Component("x5")
public class X5 extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        return false;
    }

    @Override
    public boolean isAccess() {
        return false;
    }
}
