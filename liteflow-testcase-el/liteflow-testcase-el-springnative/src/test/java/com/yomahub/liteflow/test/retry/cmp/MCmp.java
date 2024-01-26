package com.yomahub.liteflow.test.retry.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.exception.ELParseException;
import org.springframework.stereotype.Component;

@Component("m")
public class MCmp extends NodeComponent {
    int flag = 0;

    @Override
    public void process() throws Exception {
        flag ++;
        System.out.println("MCmp executed!");
        if(flag < 4) throw new ELParseException("MCmp error!");
        else flag = 0;
    }
}
