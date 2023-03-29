package com.yomahub.liteflow.test.booleanOpt.cmp;

import com.yomahub.liteflow.core.NodeIfComponent;
import org.springframework.stereotype.Component;

@Component("x2")
public class X2 extends NodeIfComponent {
    @Override
    public boolean processIf() throws Exception {
        return true;
    }
}
