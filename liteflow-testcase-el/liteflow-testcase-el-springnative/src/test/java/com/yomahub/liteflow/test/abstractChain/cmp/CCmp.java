package com.yomahub.liteflow.test.abstractChain.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;


@Component("c")
public class CCmp extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        //do your biz
        return true;
    }
}

