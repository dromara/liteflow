package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.core.NodeComponent;

@FallbackCmp
public class CommonFBCmp extends NodeComponent {

    @Override
    public void process() {
        System.out.println("CCmp executed!");
    }

}
