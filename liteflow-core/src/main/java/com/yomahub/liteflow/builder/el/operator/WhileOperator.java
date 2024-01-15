package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.WhileCondition;

/**
 * EL规则中的WHILE的操作符
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class WhileOperator extends BaseOperator<WhileCondition> {

	@Override
	public WhileCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqOne(objects);

		OperatorHelper.checkObjMustBeBooleanTypeItem(objects[0]);
		Executable whileItem = OperatorHelper.convert(objects[0], Executable.class);

		WhileCondition whileCondition = new WhileCondition();
		whileCondition.setWhileItem(whileItem);
		return whileCondition;
	}

}
