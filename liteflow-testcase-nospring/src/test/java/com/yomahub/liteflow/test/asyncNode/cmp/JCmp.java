package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeCondComponent;


public class JCmp extends NodeCondComponent {

    @Override
    public String processCond() throws Exception {
        System.out.println("Jcomp executed!");
        return "chain3";
    }
}
