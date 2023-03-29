package com.yomahub.liteflow.test.booleanOpt.cmp;

import com.yomahub.liteflow.core.NodeIfComponent;
import org.springframework.stereotype.Component;

@Component("x4")
public class X4 extends NodeIfComponent {
    @Override
    public boolean processIf() throws Exception {
        return false;
    }
}
