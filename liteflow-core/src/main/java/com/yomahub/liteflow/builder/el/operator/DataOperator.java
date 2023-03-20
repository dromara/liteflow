package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Node;

/**
 * EL规则中的data的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class DataOperator extends BaseOperator<Node> {

	@Override
	public Node build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		Node node = OperatorHelper.convert(objects[0], Node.class);

		String cmpData = OperatorHelper.convert(objects[1], String.class);

		node.setCmpData(cmpData);

		return node;
	}

}
