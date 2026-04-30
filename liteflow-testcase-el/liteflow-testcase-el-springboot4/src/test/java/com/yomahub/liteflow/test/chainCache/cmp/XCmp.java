package com.yomahub.liteflow.test.chainCache.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;

@Component("x")
public class XCmp extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        return true;
    }
}
