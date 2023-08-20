package com.yomahub.liteflow.test.rollback.cmp;

import com.yomahub.liteflow.core.NodeIfComponent;

public class XCmp extends NodeIfComponent {
    @Override
    public boolean processIf() throws Exception {
        System.out.println("XCmp executed!");
        return true;
    }

    @Override
    public void rollback() throws Exception {
        System.out.println("XCmp rollback!");
    }
}
