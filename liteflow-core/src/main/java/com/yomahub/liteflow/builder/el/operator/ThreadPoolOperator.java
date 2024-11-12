package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.condition.LoopCondition;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;

/**
 * EL规则中的threadPool的操作符 有四种用法  WHEN().threadPool() FOR...DO().threadPool() WHILE...DO.threadPool() ITERATOR...DO
 * .threadPool()
 *
 * @author Bryan.Zhang
 * @author jason
 * @since 2.8.0
 */
public class ThreadPoolOperator extends BaseOperator<Condition> {

	@Override
	public Condition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		if (objects[0] instanceof WhenCondition) {
			String errorMsg = "The caller must be WhenCondition item";

			WhenCondition condition = OperatorHelper.convert(objects[0], WhenCondition.class, errorMsg);

			condition.setThreadExecutorClass(OperatorHelper.convert(objects[1], String.class));
			return condition;
		} else if (objects[0] instanceof LoopCondition) {
			String errorMsg = "The caller must be LoopCondition item";

			LoopCondition condition = OperatorHelper.convert(objects[0], LoopCondition.class, errorMsg);

			condition.setThreadPoolExecutorClass(OperatorHelper.convert(objects[1], String.class));
			return condition;
		} else {
            String errorMsg = "The caller must be WhenCondition or LoopCondition item";
			throw new QLException(errorMsg);
		}
	}

}
