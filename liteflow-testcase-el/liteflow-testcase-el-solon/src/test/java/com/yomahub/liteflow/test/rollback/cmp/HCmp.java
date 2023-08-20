package com.yomahub.liteflow.test.rollback.cmp;

import com.yomahub.liteflow.core.NodeBreakComponent;
import org.noear.solon.annotation.Component;

@Component("h")
public class HCmp extends NodeBreakComponent {

    @Override
    public boolean processBreak() throws Exception {
        System.out.println("HCmp executed!");
        throw new RuntimeException();
    }

    @Override
    public void rollback() throws Exception {
        System.out.println("HCmp rollback!");
    }
}
