package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("n")
public class NCmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        String seconds = this.getTag();
        Thread.sleep((long) (1000 * Double.parseDouble(seconds)));
        // 手动抛异常
        System.out.println("Ncomp executed with exeption!");
        int a = 1 / 0;
    }

}
