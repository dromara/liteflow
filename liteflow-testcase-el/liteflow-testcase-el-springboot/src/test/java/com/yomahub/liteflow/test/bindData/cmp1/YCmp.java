package com.yomahub.liteflow.test.bindData.cmp1;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("y")
public class YCmp extends NodeSwitchComponent {
    @Override
    public String processSwitch() throws Exception {
        DefaultContext context = this.getFirstContextBean();
        String bindValue = this.getBindData("k1", String.class);
        context.setData(this.getNodeId(), bindValue);
        return "d";
    }
}
