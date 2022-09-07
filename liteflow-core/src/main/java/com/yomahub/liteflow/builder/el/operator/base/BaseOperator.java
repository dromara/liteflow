package com.yomahub.liteflow.builder.el.operator.base;

import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.exception.ELParseException;

/**
 * BaseOperator 为了强化 executeInner 方法，会捕获抛出的 QLException 错误，输出友好的错误提示
 *
 * @author gaibu
 * @since 2.8.6
 */
public abstract class BaseOperator extends Operator {

	@Override
	public Object executeInner(Object[] objects) throws Exception {
		try {
			return buildCondition(objects);
		} catch (QLException e) {
			throw e;
		} catch (Exception e) {
			throw new ELParseException("errors occurred in EL parsing");
		}
	}

	/**
	 * 构建 EL 条件
	 *
	 * @param objects objects
	 * @return Condition
	 * @throws Exception Exception
	 */
	public abstract Object buildCondition(Object[] objects) throws Exception;
}
