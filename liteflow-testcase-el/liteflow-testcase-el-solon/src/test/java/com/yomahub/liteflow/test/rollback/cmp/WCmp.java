package com.yomahub.liteflow.test.rollback.cmp;

import com.yomahub.liteflow.core.NodeWhileComponent;
import org.noear.solon.annotation.Component;

@Component("w")
public class WCmp extends NodeWhileComponent {

    @Override
    public boolean processWhile() throws Exception {
        System.out.println("WCmp executed!");
        return true;
    }

    @Override
    public void rollback() throws Exception {
        System.out.println("WCmp rollback!");
    }
}
