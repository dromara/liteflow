package com.yomahub.liteflow.test.rollback.cmp;

import com.yomahub.liteflow.annotation.LiteflowRetry;
import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;

@Component("m")
@LiteflowRetry(5)
public class MCmp extends NodeComponent {

    private int flag = 0;
    @Override
    public void process() {
        if(flag < 2) {
            flag ++;
            throw new RuntimeException();
        }
        System.out.println("MCmp executed!");
    }

    @Override
    public void rollback() throws Exception {
        System.out.println("MCmp rollback!");
    }
}
