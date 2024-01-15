package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;

/**
 * EL规则中的threadPool的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class ThreadPoolOperator extends BaseOperator<WhenCondition> {

	@Override
	public WhenCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		String errorMsg = "The caller must be WhenCondition item";
		WhenCondition whenCondition = OperatorHelper.convert(objects[0], WhenCondition.class, errorMsg);

		whenCondition.setThreadExecutorClass(OperatorHelper.convert(objects[1], String.class));

		return whenCondition;
	}

}
