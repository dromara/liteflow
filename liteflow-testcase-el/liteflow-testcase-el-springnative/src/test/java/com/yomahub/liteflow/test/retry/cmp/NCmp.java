package com.yomahub.liteflow.test.retry.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;

@Component("n")
public class NCmp extends NodeBooleanComponent {
    int flag = 0;

    @Override
    public boolean processBoolean() throws Exception {
        flag ++;
        System.out.println("NCmp executed!");
        if(flag < 4) throw new RuntimeException();
        else return flag == 4 ? true : false;
    }
}
