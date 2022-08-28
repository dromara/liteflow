package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.collection.ListUtil;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.IfCondition;

/**
 * EL规则中的ELIF的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public class ElifOperator extends BaseOperator {

	@Override
	public Object buildCondition(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqThree(objects);

		//解析caller
		IfCondition ifCondition = OperatorHelper.convert(objects[0], IfCondition.class);

		//解析第一个参数
		Node ifNode;
		if (objects[1] instanceof Node) {
			ifNode = (Node) objects[1];

			if (!ListUtil.toList(NodeTypeEnum.IF, NodeTypeEnum.IF_SCRIPT).contains(ifNode.getType())) {
				throw new QLException("The first parameter must be If item");
			}
		} else {
			throw new QLException("The first parameter must be Node item");
		}

		//解析第二个参数
		Executable trueCaseExecutableItem = (Executable) objects[2];

		//构建一个内部的IfCondition
		IfCondition ifConditionItem = new IfCondition();
		ifConditionItem.setExecutableList(ListUtil.toList(ifNode));
		ifConditionItem.setTrueCaseExecutableItem(trueCaseExecutableItem);

		//因为可能会有多个ELIF，所以每一次拿到的caller总是最开始大的if，需要遍历到没有falseCaseExecutable的地方。
		//塞进去是一个新的IfCondition
		IfCondition loopIfCondition = ifCondition;
		while (true) {
			if (loopIfCondition.getFalseCaseExecutableItem() == null) {
				loopIfCondition.setFalseCaseExecutableItem(ifConditionItem);
				break;
			} else {
				loopIfCondition = (IfCondition) loopIfCondition.getFalseCaseExecutableItem();
			}
		}

		return ifCondition;
	}
}
