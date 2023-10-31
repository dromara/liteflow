package com.yomahub.liteflow.test.rollback.cmp;

import com.yomahub.liteflow.annotation.LiteflowRetry;
import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;

@Component("n")
@LiteflowRetry(3)
public class NCmp extends NodeComponent {
    @Override
    public void process() {
        System.out.println("NCmp executed!");
        throw new RuntimeException();
    }

    @Override
    public void rollback() throws Exception {
        System.out.println("NCmp rollback!");
    }
}
