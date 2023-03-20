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
		OperatorHelper.checkObjectSizeGtTwo(objects);

		SwitchCondition switchCondition = OperatorHelper.convert(objects[0], SwitchCondition.class);

		for (int i = 1; i < objects.length; i++) {
			Executable target = OperatorHelper.convert(objects[i], Executable.class);
			switchCondition.addTargetItem(target);
		}
		return switchCondition;
	}

}
