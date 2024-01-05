package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.LoopCondition;

/**
 * EL规则中的BREAK的操作符 有两种用法 FOR...DO...BREAK WHILE...DO...BREAK
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class BreakOperator extends BaseOperator<LoopCondition> {

	@Override
	public LoopCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		// DO关键字有可能用在FOR后面，也有可能用于WHILE后面，所以这里要进行判断是不是这两种类型的超类LoopCondition
		String errorMsg = "The caller must be ForCondition or WhileCondition item";
		LoopCondition condition = OperatorHelper.convert(objects[0], LoopCondition.class, errorMsg);

		// 获得需要执行的可执行表达式
		OperatorHelper.checkObjMustBeBooleanTypeItem(objects[1]);
		Executable breakItem = OperatorHelper.convert(objects[1], Executable.class);
		condition.setBreakItem(breakItem);
		return condition;
	}

}
