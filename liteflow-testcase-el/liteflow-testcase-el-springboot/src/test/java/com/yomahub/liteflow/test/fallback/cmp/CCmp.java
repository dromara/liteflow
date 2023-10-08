package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@LiteflowComponent("c")
@FallbackCmp
public class CCmp extends NodeComponent {

    @Override
    public void process() {
        System.out.println("CCmp executed!");
    }

}
