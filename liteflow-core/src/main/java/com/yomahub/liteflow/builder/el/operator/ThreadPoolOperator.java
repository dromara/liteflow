package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.exception.QLException;
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

		WhenCondition whenCondition = OperatorHelper.convert(objects[0], WhenCondition.class);

		if (objects[1] instanceof String) {
			// threadPoolClazz
			whenCondition.setThreadExecutorClass(objects[1].toString());
		} else {
			throw new QLException("the parameter must be String type");
		}

		return whenCondition;
	}
}
