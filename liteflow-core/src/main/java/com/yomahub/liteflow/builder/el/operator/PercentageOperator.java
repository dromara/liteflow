package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.enums.ParallelStrategyEnum;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;

/**
 * EL 规则中的 percentage 的操作符
 *
 * @author luo yi
 * @since 2.13.4
 */
public class PercentageOperator extends BaseOperator<WhenCondition> {

    @Override
    public WhenCondition build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeEqTwo(objects);

        WhenCondition whenCondition = OperatorHelper.convert(objects[0], WhenCondition.class, "The caller must be WhenCondition item");

        // 指定并行任务需要完成的阈值
        Double percentage = OperatorHelper.convert2Double(objects[1]);

        if (percentage > 1 || percentage < 0) {
            throw new ELParseException("The percentage must be between 0 and 1.");
        }

        whenCondition.setParallelStrategy(ParallelStrategyEnum.PERCENTAGE);
        whenCondition.setPercentage(percentage);
        return whenCondition;
    }

}
