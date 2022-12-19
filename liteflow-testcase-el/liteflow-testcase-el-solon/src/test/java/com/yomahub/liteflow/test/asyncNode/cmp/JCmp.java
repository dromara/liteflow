package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.noear.solon.annotation.Component;


@Component("j")
public class JCmp extends NodeSwitchComponent {

    @Override
    public String processSwitch() throws Exception {
        System.out.println("Jcomp executed!");
        return "chain3";
    }
}
