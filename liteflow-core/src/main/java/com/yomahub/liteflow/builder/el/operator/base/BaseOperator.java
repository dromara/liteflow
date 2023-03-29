package com.yomahub.liteflow.builder.el.operator.base;

import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.element.Executable;

/**
 * BaseOperator 为了强化 executeInner 方法，会捕获抛出的 QLException 错误，输出友好的错误提示
 *
 * @author gaibu
 * @since 2.8.6
 */
public abstract class BaseOperator<T extends Executable> extends Operator {

	@Override
	public T executeInner(Object[] objects) throws Exception {
		try {
			OperatorHelper.checkItemNotNull(objects);
			return build(objects);
		}
		catch (QLException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ELParseException("errors occurred in EL parsing");
		}
	}

	/**
	 * 构建 EL 条件
	 * @param objects objects
	 * @return Condition
	 * @throws Exception Exception
	 */
	public abstract T build(Object[] objects) throws Exception;

}
