package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.SwitchCondition;

/**
 * EL规则中的default的操作符，用法须和SWITCH联合使用
 *
 * @author Tingliang Wang
 * @since 2.9.5
 */
public class DefaultOperator extends BaseOperator<SwitchCondition> {

	@Override
	public SwitchCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		String errorMsg = "The caller must be SwitchCondition item";
		SwitchCondition switchCondition = OperatorHelper.convert(objects[0], SwitchCondition.class, errorMsg);

		OperatorHelper.checkObjMustBeCommonTypeItem(objects[1]);

		Executable target = OperatorHelper.convert(objects[1], Executable.class);

		switchCondition.setDefaultExecutor(target);

		return switchCondition;
	}

}
