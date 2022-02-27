package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeCondComponent;


public class ECmp extends NodeCondComponent {

    @Override
    public String processCond() throws Exception {
        System.out.println("Ecomp executed!");
        return "g";
    }
}
