package com.yomahub.liteflow.test.condition.cmp1;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;


@Component("f")
public class FCmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        System.out.println("Fcomp executed!");
    }
}
