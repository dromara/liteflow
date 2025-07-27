package com.yomahub.liteflow.test.chainCache.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.noear.solon.annotation.Component;

@Component("x")
public class XCmp extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        return true;
    }
}
