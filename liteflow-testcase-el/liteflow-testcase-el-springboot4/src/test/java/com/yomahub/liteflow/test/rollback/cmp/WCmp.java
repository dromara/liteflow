package com.yomahub.liteflow.test.rollback.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;

@Component("w")
public class WCmp extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() throws Exception {
        System.out.println("WCmp executed!");
        return true;
    }

    @Override
    public void rollback() throws Exception {
        System.out.println("WCmp rollback!");
    }
}
