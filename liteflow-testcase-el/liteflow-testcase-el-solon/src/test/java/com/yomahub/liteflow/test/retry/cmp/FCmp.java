package com.yomahub.liteflow.test.retry.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.noear.solon.annotation.Component;

@Component("f")
public class FCmp extends NodeBooleanComponent {
    int flag = 0;
    @Override
    public boolean processBoolean() throws Exception {
        System.out.println("FCmp executed!");
        flag ++;
        if(flag < 4) throw new RuntimeException();
        else return true;
    }
}
