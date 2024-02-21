package com.yomahub.liteflow.test.booleanOpt.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;

@Component("x3")
public class X3 extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        return false;
    }
}
