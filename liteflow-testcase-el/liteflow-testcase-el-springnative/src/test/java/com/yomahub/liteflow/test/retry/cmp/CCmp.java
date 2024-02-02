package com.yomahub.liteflow.test.retry.cmp;

import com.yomahub.liteflow.core.NodeForComponent;
import org.springframework.stereotype.Component;

@Component("c")
public class CCmp extends NodeForComponent {
    int flag = 0;

    @Override
    public int processFor() throws Exception {
        flag ++;
        System.out.println("CCmp executed!");
        if(flag < 4) throw new RuntimeException();
        else return 1;
    }
}
