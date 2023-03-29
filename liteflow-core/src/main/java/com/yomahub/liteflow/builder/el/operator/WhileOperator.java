package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.collection.ListUtil;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.WhileCondition;

/**
 * EL规则中的WHILE的操作符
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class WhileOperator extends BaseOperator<WhileCondition> {

	@Override
	public WhileCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqOne(objects);

		Executable whileItem = OperatorHelper.convert(objects[0], Executable.class);
		OperatorHelper.checkObjectMustBeBooleanItem(whileItem);

		WhileCondition whileCondition = new WhileCondition();
		whileCondition.setWhileItem(whileItem);
		return whileCondition;
	}

}
