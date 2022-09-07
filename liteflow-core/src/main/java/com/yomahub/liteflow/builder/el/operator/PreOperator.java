package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.PreCondition;

/**
 * EL规则中的THEN的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class PreOperator extends BaseOperator {

	@Override
	public Object buildCondition(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeGtZero(objects);

		PreCondition preCondition = new PreCondition();
		for (Object obj : objects) {
			if (obj instanceof Executable) {
				preCondition.addExecutable((Executable) obj);
			} else {
				throw new QLException("parameter must be executable item");
			}
		}
		return preCondition;
	}
}
