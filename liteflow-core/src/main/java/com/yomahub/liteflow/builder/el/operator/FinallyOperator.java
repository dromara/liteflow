package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.FinallyCondition;

/**
 * EL规则中的THEN的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class FinallyOperator extends BaseOperator<FinallyCondition> {

	@Override
	public FinallyCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeGtZero(objects);

		FinallyCondition finallyCondition = new FinallyCondition();
		for (Object obj : objects) {
			OperatorHelper.checkObjMustBeCommonTypeItem(obj);
			Executable item = OperatorHelper.convert(obj, Executable.class);
			finallyCondition.addExecutable(item);
		}
		return finallyCondition;
	}

}
