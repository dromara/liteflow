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
 * EL规则中的IF的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public class IfOperator extends BaseOperator<IfCondition> {

	@Override
	public IfCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEq(objects, 2, 3);

		//解析第一个参数
		Node ifNode;
		if (objects[0] instanceof Node) {
			ifNode = (Node) objects[0];

			if (!ListUtil.toList(NodeTypeEnum.IF, NodeTypeEnum.IF_SCRIPT).contains(ifNode.getType())) {
				throw new QLException("The first parameter must be If item");
			}
		} else {
			throw new QLException("The first parameter must be Node item");
		}

		//解析第二个参数
		Executable trueCaseExecutableItem = (Executable) objects[1];

		//解析第三个参数，如果有的话
		Executable falseCaseExecutableItem = null;
		if (objects.length == 3) {
			falseCaseExecutableItem = (Executable) objects[2];
		}

		IfCondition ifCondition = new IfCondition();
		ifCondition.setExecutableList(ListUtil.toList(ifNode));
		ifCondition.setTrueCaseExecutableItem(trueCaseExecutableItem);
		ifCondition.setFalseCaseExecutableItem(falseCaseExecutableItem);
		return ifCondition;
	}
}
