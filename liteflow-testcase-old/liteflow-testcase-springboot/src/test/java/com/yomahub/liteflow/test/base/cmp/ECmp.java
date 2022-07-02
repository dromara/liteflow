package com.yomahub.liteflow.test.base.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.springframework.stereotype.Component;

@Component("e")
public class ECmp extends NodeSwitchComponent {
    @Override
    public String processSwitch() throws Exception {
        return "d";
    }
}
