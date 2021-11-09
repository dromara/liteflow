package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;


@Component("g")
public class GCmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        System.out.println("Gcomp executed!");
    }
}
