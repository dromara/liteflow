package com.yomahub.liteflow.test.script.graaljs.getnodes.cmp;

import com.yomahub.liteflow.core.NodeIfComponent;
import org.springframework.stereotype.Component;

@Component("f")
public class FCmp extends NodeIfComponent {
    @Override
    public boolean processIf() throws Exception {
        return true;
    }
}
