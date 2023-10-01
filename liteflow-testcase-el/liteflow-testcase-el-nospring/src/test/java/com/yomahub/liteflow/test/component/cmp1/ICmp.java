package com.yomahub.liteflow.test.component.cmp1;

import com.yomahub.liteflow.core.NodeComponent;

public class ICmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        System.out.println("process i");
        throw new RuntimeException();
    }

    @Override
    public boolean isContinueOnError() {
        return true;
    }

    @Override
    public boolean isEnd() {
        return true;
    }
}
