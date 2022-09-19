package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.condition.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EL规则中的id的操作符,只有condition可加id
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class IdOperator extends BaseOperator<Condition> {

	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	@Override
	public Condition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		Condition condition = OperatorHelper.convert(objects[0], Condition.class);

		if (objects[1] instanceof String) {
			// id
			condition.setId(objects[1].toString());
		} else {
			LOG.error("the parameter must be String type!");
			throw new QLException("the parameter must be String type");
		}

		return condition;
	}
}
