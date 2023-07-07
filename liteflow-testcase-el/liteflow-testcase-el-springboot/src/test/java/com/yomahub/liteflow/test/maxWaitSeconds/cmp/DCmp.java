package com.yomahub.liteflow.test.maxWaitSeconds.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;

import static com.yomahub.liteflow.test.maxWaitSeconds.MaxWaitSecondsELSpringBootTest.CONTENT_KEY;

@LiteflowComponent("d")
public class DCmp  extends NodeComponent {

    @Override
    public void process() {
        try {
            Thread.sleep(500);
            DefaultContext contextBean = this.getFirstContextBean();
            contextBean.setData(CONTENT_KEY,"value");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("DCmp executed!");
    }
}
