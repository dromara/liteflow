package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;

/**
 * EL规则中的ignoreError的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class IgnoreErrorOperator extends BaseOperator<WhenCondition> {

	@Override
	public WhenCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		WhenCondition condition = OperatorHelper.convert(objects[0], WhenCondition.class);

		if (objects[1] instanceof Boolean) {
			// ignoreError
			condition.setErrorResume(Boolean.parseBoolean(objects[1].toString().toLowerCase()));
		} else {
			throw new QLException("The parameter must be boolean type");
		}

		return condition;
	}
}
