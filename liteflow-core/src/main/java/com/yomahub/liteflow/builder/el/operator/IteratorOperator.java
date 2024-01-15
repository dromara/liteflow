package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.collection.ListUtil;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.IteratorCondition;

public class IteratorOperator extends BaseOperator<IteratorCondition> {

	@Override
	public IteratorCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEq(objects, 1);

		OperatorHelper.checkObjMustBeIteratorTypeItem(objects[0]);

		Node node = OperatorHelper.convert(objects[0], Node.class);

		IteratorCondition iteratorCondition = new IteratorCondition();
		iteratorCondition.setIteratorNode(node);

		return iteratorCondition;
	}

}
