package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Condition;

/**
 * EL规则中的id的操作符,只有condition可加id
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class IdOperator extends BaseOperator<Condition> {

	@Override
	public Condition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		String errorMsg = "The caller must be Condition item";
		Condition condition = OperatorHelper.convert(objects[0], Condition.class, errorMsg);

		String id = OperatorHelper.convert(objects[1], String.class);

		condition.setId(id);

		return condition;
	}

}
