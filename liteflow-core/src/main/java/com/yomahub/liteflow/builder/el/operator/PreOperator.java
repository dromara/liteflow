package com.yomahub.liteflow.builder.el.operator;

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
public class PreOperator extends BaseOperator<PreCondition> {

	@Override
	public PreCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeGtZero(objects);

		PreCondition preCondition = new PreCondition();
		for (Object obj : objects) {
			OperatorHelper.checkObjMustBeCommonTypeItem(obj);
			preCondition.addExecutable(OperatorHelper.convert(obj, Executable.class));
		}
		return preCondition;
	}

}
