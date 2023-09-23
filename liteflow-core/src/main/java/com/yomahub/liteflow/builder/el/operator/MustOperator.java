package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.enums.ParallelStrategyEnum;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;

/**
 * EL 规则中的 must 的操作符
 *
 * @author luo yi
 * @since 2.11.0
 */
public class MustOperator extends BaseOperator<WhenCondition> {

	@Override
	public WhenCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		WhenCondition whenCondition = OperatorHelper.convert(objects[0], WhenCondition.class);

		String specifyId = OperatorHelper.convert(objects[1], String.class);
		whenCondition.setSpecifyId(specifyId);
		whenCondition.setParallelStrategy(ParallelStrategyEnum.SPECIFY);
		return whenCondition;
	}

}
