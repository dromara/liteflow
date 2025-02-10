package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;

/**
 * EL规则中的data的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class DataOperator extends BaseOperator<Executable> {

	@Override
	public Executable build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		Executable item = OperatorHelper.convert(objects[0], Executable.class);

		String cmpData = OperatorHelper.convert(objects[1], String.class);

		LiteflowMetaOperator.getNodes(item).forEach(node -> node.setCmpData(cmpData));

		return item;
	}

}
