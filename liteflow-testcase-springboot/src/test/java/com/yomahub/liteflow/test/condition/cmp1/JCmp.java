package com.yomahub.liteflow.test.condition.cmp1;

import com.yomahub.liteflow.core.NodeCondComponent;
import org.springframework.stereotype.Component;


@Component("j")
public class JCmp extends NodeCondComponent {

    @Override
    public String processCond() throws Exception {
        System.out.println("Jcomp executed!");
        return "chain3";
    }
}
