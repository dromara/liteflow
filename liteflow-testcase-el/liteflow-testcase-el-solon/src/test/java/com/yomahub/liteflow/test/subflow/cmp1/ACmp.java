package com.yomahub.liteflow.test.subflow.cmp1;

import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;


@Component("a")
public class ACmp extends NodeComponent {
    @Override
    public void process() {
        System.out.println("Acomp executed!");
    }
}
