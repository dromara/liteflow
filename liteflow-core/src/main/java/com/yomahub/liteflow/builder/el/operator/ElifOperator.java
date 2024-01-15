package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.IfCondition;

/**
 * EL规则中的ELIF的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public class ElifOperator extends BaseOperator<IfCondition> {

	@Override
	public IfCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqThree(objects);

		// 解析caller
		String errorMsg = "The caller must be IfCondition item";
		IfCondition ifCondition = OperatorHelper.convert(objects[0], IfCondition.class, errorMsg);

		// 解析第一个参数
		OperatorHelper.checkObjMustBeBooleanTypeItem(objects[1]);
		Executable ifItem = OperatorHelper.convert(objects[1], Executable.class);

		// 解析第二个参数
		OperatorHelper.checkObjMustBeCommonTypeItem(objects[2]);
		Executable trueCaseExecutableItem = OperatorHelper.convert(objects[2], Executable.class);

		// 构建一个内部的IfCondition
		IfCondition ifConditionItem = new IfCondition();
		ifConditionItem.setIfItem(ifItem);
		ifConditionItem.setTrueCaseExecutableItem(trueCaseExecutableItem);

		// 因为可能会有多个ELIF，所以每一次拿到的caller总是最开始大的if，需要遍历到没有falseCaseExecutable的地方。
		// 塞进去是一个新的IfCondition
		IfCondition loopIfCondition = ifCondition;
		while (true) {
			if (loopIfCondition.getFalseCaseExecutableItem() == null) {
				loopIfCondition.setFalseCaseExecutableItem(ifConditionItem);
				break;
			}
			else {
				loopIfCondition = (IfCondition) loopIfCondition.getFalseCaseExecutableItem();
			}
		}

		return ifCondition;
	}

}
