package com.yomahub.liteflow.test.rollback.cmp;

import com.yomahub.liteflow.core.NodeForComponent;

public class GCmp extends NodeForComponent {

    @Override
    public int processFor() throws Exception {
        System.out.println("GCmp executed!");
        return 3;
    }

    @Override
    public void rollback() throws Exception {
        System.out.println("GCmp rollback!");
    }
}
