package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.condition.LoopCondition;

/**
 * EL规则中的parallel的操作符
 *
 * @author zhhhhy
 * @since 2.11.0
 */

public class ParallelOperator extends BaseOperator<LoopCondition> {
    @Override
    public LoopCondition build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeEqTwo(objects);

        String errorMsg = "The caller must be LoopCondition item";
        LoopCondition loopCondition = OperatorHelper.convert(objects[0], LoopCondition.class, errorMsg);

        Boolean parallel = OperatorHelper.convert(objects[1], Boolean.class);
        loopCondition.setParallel(parallel);
        return loopCondition;
    }
}
