package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.IfCondition;

/**
 * EL规则中的ELSE的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public class ElseOperator extends BaseOperator<IfCondition> {

	@Override
	public IfCondition build(Object[] objects) throws Exception {
		// 参数只能是1个，但这里为什么是2个呢？第一个是caller，第二个才是参数
		OperatorHelper.checkObjectSizeEqTwo(objects);

		String errorMsg = "The caller must be IfCondition item";
		IfCondition ifCondition = OperatorHelper.convert(objects[0], IfCondition.class, errorMsg);

		OperatorHelper.checkObjMustBeCommonTypeItem(objects[1]);
		Executable elseExecutableItem = OperatorHelper.convert(objects[1], Executable.class);

		// 因为当中可能会有多个ELIF，所以并不知道这个ELSE前面有没有ELIF，
		// 每一次拿到的caller总是最开始大的if，需要遍历到没有falseCaseExecutable的地方。
		// 塞进去是一个elseExecutableItem
		IfCondition loopIfCondition = ifCondition;
		while (true) {
			if (loopIfCondition.getFalseCaseExecutableItem() == null) {
				loopIfCondition.setFalseCaseExecutableItem(elseExecutableItem);
				break;
			}
			else {
				loopIfCondition = (IfCondition) loopIfCondition.getFalseCaseExecutableItem();
			}
		}

		return ifCondition;
	}

}
