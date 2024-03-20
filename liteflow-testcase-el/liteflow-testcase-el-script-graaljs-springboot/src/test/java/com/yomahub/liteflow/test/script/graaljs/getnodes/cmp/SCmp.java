package com.yomahub.liteflow.test.script.graaljs.getnodes.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.springframework.stereotype.Component;

@Component("s")
public class SCmp extends NodeSwitchComponent {
    @Override
    public String processSwitch() throws Exception {
        return "f1";
    }
}
