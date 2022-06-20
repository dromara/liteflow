package com.yomahub.liteflow.test.base.cmp;

import com.yomahub.liteflow.core.NodeCondComponent;
import org.springframework.stereotype.Component;

@Component("e")
public class ECmp extends NodeCondComponent {
    @Override
    public String processCond() throws Exception {
        return "d";
    }
}
