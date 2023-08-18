package com.yomahub.liteflow.test.rollback.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.noear.solon.annotation.Component;

@Component("f")
public class FCmp extends NodeSwitchComponent {

    @Override
    public String processSwitch() {
        System.out.println("FCmp executed!");
        return "abc";
    }

    @Override
    public void rollback() throws Exception {
        System.out.println("FCmp rollback!");
    }
}
