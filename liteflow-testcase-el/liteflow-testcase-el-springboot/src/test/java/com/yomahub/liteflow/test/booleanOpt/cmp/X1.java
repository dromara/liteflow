package com.yomahub.liteflow.test.booleanOpt.cmp;

import com.yomahub.liteflow.core.NodeIfComponent;
import org.springframework.stereotype.Component;

@Component("x1")
public class X1 extends NodeIfComponent {
    @Override
    public boolean processIf() throws Exception {
        return true;
    }
}
