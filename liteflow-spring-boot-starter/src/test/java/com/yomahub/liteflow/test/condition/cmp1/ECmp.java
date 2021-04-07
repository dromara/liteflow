package com.yomahub.liteflow.test.condition.cmp1;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.NodeCondComponent;
import org.springframework.stereotype.Component;


@Component("e")
public class ECmp extends NodeCondComponent {

    @Override
    public String processCond() throws Exception {
        System.out.println("Ecomp executed!");
        return "g";
    }
}
