package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.CatchCondition;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.condition.LoopCondition;

/**
 * EL规则中的DO的操作符 有三种用法 FOR...DO...BREAK WHILE...DO...BREAK CATCH...DO
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class DoOperator extends BaseOperator<Condition> {

	@Override
	public Condition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		if (objects[0] instanceof CatchCondition) {
			String errorMsg = "The caller must be CatchCondition item";
			CatchCondition condition = OperatorHelper.convert(objects[0], CatchCondition.class, errorMsg);
			// 获得需要执行的可执行表达式
			OperatorHelper.checkObjMustBeCommonTypeItem(objects[1]);
			Executable doExecutableItem = OperatorHelper.convert(objects[1], Executable.class);
			condition.setDoItem(doExecutableItem);
			return condition;
		}
		else if (objects[0] instanceof LoopCondition) {
			String errorMsg = "The caller must be LoopCondition item";
			// DO关键字有可能用在FOR后面，也有可能用于WHILE后面，所以这里要进行判断是不是这两种类型的超类LoopCondition
			LoopCondition condition = OperatorHelper.convert(objects[0], LoopCondition.class, errorMsg);
			// 获得需要执行的可执行表达式
			OperatorHelper.checkObjMustBeCommonTypeItem(objects[1]);
			Executable doExecutableItem = OperatorHelper.convert(objects[1], Executable.class);
			condition.setDoExecutor(doExecutableItem);
			return condition;
		}
		else {
			String errorMsg = "The caller must be LoopCondition or CatchCondition item";
			throw new QLException(errorMsg);
		}
	}

}
