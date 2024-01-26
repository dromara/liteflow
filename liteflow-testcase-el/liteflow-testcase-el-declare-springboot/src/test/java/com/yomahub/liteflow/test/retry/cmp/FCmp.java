package com.yomahub.liteflow.test.retry.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeIfComponent;

@LiteflowComponent("f")
public class FCmp extends NodeIfComponent {
    int flag = 0;
    @Override
    public boolean processIf() throws Exception {
        System.out.println("FCmp executed!");
        flag ++;
        if(flag < 4) throw new RuntimeException();
        else return true;
    }
}
