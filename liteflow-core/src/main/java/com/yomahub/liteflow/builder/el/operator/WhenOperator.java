package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;

/**
 * EL规则中的WHEN的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class WhenOperator extends BaseOperator {

	@Override
	public Object buildCondition(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeGtZero(objects);

		WhenCondition whenCondition = new WhenCondition();
		for (Object obj : objects) {
			if (obj instanceof Executable) {
				whenCondition.addExecutable((Executable) obj);
			} else {
				throw new QLException("parameter error");
			}
		}
		return whenCondition;
	}
}
