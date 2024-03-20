package com.yomahub.liteflow.test.rollback.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;

public class HCmp extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() throws Exception {
        System.out.println("HCmp executed!");
        throw new RuntimeException();
    }

    @Override
    public void rollback() throws Exception {
        System.out.println("HCmp rollback!");
    }
}
