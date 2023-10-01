package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.enums.ParallelStrategyEnum;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;

/**
 * EL规则中的any的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class AnyOperator extends BaseOperator<WhenCondition> {

	@Override
	public WhenCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		WhenCondition whenCondition = OperatorHelper.convert(objects[0], WhenCondition.class);

		Boolean any = OperatorHelper.convert(objects[1], Boolean.class);
		whenCondition.setParallelStrategy(any ? ParallelStrategyEnum.ANY : ParallelStrategyEnum.ALL);
		return whenCondition;
	}

}
