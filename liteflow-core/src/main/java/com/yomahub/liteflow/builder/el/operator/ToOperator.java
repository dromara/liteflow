package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.exception.QLException;
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
			if (objects[i] instanceof Executable) {
				Executable target = (Executable) objects[i];
				switchCondition.addTargetItem(target);
			} else {
				throw new QLException("The parameter must be Executable item");
			}
		}
		return switchCondition;
	}
}
