package com.yomahub.liteflow.test.subflow.cmp1;

import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;


@Component("e")
public class ECmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        System.out.println("Ecomp executed!");
    }
}
