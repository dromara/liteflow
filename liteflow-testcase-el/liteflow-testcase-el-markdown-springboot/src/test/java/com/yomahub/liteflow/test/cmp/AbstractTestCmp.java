package com.yomahub.liteflow.test.cmp;

import com.yomahub.liteflow.core.NodeComponent;

public abstract class AbstractTestCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        String tag = this.getTag();
        System.out.println(this.getClass().getName() + " executed! > tag:" + tag);
    }
}
