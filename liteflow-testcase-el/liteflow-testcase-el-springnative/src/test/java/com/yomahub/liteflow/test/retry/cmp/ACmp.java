package com.yomahub.liteflow.test.retry.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("a")
public class ACmp extends NodeComponent {
    @Override
    public void process() {
        System.out.println("ACmp executed!");
    }
}
