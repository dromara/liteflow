package com.yomahub.liteflow.test.subflow2.cmp1;

import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;


@Component("d")
public class DCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        System.out.println("Dcomp executed!");
    }
}
