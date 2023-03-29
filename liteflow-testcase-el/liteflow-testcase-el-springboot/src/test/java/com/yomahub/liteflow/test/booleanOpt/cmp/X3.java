package com.yomahub.liteflow.test.booleanOpt.cmp;

import com.yomahub.liteflow.core.NodeIfComponent;
import org.springframework.stereotype.Component;

@Component("x3")
public class X3 extends NodeIfComponent {
    @Override
    public boolean processIf() throws Exception {
        return false;
    }
}
