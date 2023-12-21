package com.yomahub.liteflow.test.maxWaitMilliseconds.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;


public class DCmp extends NodeComponent {

    public static final String CONTENT_KEY = "testKey";

    @Override
    public void process() {
        try {
            Thread.sleep(500);
            DefaultContext contextBean = this.getFirstContextBean();
            contextBean.setData(CONTENT_KEY, "value");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("DCmp executed!");
    }
}
