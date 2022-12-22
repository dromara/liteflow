package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.noear.solon.annotation.Component;


@Component("e")
public class ECmp extends NodeSwitchComponent {

    @Override
    public String processSwitch() throws Exception {
        System.out.println("Ecomp executed!");
        return "g";
    }
}
