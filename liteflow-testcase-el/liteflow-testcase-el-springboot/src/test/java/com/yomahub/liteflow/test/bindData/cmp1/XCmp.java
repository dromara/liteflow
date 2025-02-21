package com.yomahub.liteflow.test.bindData.cmp1;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("x")
public class XCmp extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        DefaultContext context = this.getFirstContextBean();
        String bindValue = this.getBindData("k1", String.class);
        context.setData(this.getNodeId(), bindValue);
        return true;
    }
}
