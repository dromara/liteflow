package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Node;

/**
 * EL规则中的tag的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class TagOperator extends BaseOperator {

	@Override
	public Object buildCondition(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		Node node = OperatorHelper.convert(objects[0], Node.class);

		String tag ;
		if (objects[1] instanceof String) {
			tag = objects[1].toString();
		} else {
			throw new QLException("the parameter must be String type");
		}

		//这里为什么要clone一个呢？
		//因为tag是跟着chain走的。而在el上下文里的放的都是同一个node，如果多个同样的node tag不同，则这里必须copy
		Node copyNode = FlowBus.copyNode(node.getId());
		if (null == copyNode){
			throw new QLException("The Node must be not null");
		}
		copyNode.setTag(tag);

		return copyNode;
	}
}
