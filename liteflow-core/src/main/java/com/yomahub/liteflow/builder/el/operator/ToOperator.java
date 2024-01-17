package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.SwitchCondition;

/**
 * EL规则中的TO的操作符，用法须和SWITCH联合使用
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class ToOperator extends BaseOperator<SwitchCondition> {

	@Override
	public SwitchCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeGteTwo(objects);

		String errorMsg = "The caller must be SwitchCondition item";
		SwitchCondition switchCondition = OperatorHelper.convert(objects[0], SwitchCondition.class, errorMsg);

		for (int i = 1; i < objects.length; i++) {
			OperatorHelper.checkObjMustBeCommonTypeItem(objects[i]);
			Executable target = OperatorHelper.convert(objects[i], Executable.class);
			switchCondition.addTargetItem(target);
		}
		return switchCondition;
	}

}
