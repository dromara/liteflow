package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import com.yomahub.liteflow.enums.BooleanTypeEnum;

@LiteflowComponent("bn1")
@FallbackCmp(BooleanTypeEnum.BREAK)
public class BreakCmp extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() throws Exception {
        return true;
    }
}
