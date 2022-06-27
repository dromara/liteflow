package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;


public class ECmp extends NodeSwitchComponent {

    @Override
    public String processCond() throws Exception {
        System.out.println("Ecomp executed!");
        return "g";
    }
}
