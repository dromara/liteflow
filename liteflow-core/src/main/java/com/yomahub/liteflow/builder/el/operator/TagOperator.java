package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;

/**
 * EL规则中的tag的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class TagOperator extends BaseOperator<Executable> {

	@Override
	public Executable build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		Executable refObj = OperatorHelper.convert(objects[0], Executable.class);

		String tag = OperatorHelper.convert(objects[1], String.class);

		refObj.setTag(tag);

		return refObj;
	}

}
