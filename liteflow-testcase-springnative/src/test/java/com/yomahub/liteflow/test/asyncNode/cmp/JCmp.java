package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.springframework.stereotype.Component;


@Component("j")
public class JCmp extends NodeSwitchComponent {

    @Override
    public String processCond() throws Exception {
        System.out.println("Jcomp executed!");
        return "chain3";
    }
}
