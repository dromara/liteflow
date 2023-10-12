package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.ChainNotImplementedException;
import com.yomahub.liteflow.flow.element.Condition;

/**
 * AbstractCondition,用于标记一个Chain为不可执行的抽象Chain
 *
 * @author zy
 * @since 2.11.1
 */

public class AbstractCondition extends Condition {
    @Override
    public void executeCondition(Integer slotIndex) throws Exception {
        throw new ChainNotImplementedException(StrUtil.format("chain[{}] contains unimplemented variables, cannot be executed", this.getCurrChainId()));
    }

    @Override
    public ConditionTypeEnum getConditionType() {
        return ConditionTypeEnum.TYPE_ABSTRACT;
    }
}
