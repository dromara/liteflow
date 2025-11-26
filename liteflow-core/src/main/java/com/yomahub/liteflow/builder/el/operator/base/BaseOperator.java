package com.yomahub.liteflow.builder.el.operator.base;

import com.alibaba.qlexpress4.api.QLFunctionalVarargs;
import com.alibaba.qlexpress4.exception.QLException;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.element.Executable;

/**
 * BaseOperator 为了强化 call 方法，会捕获抛出的 QLException 错误，输出友好的错误提示
 *
 * @author gaibu
 * @since 2.8.6
 */
public abstract class BaseOperator<T extends Executable> implements QLFunctionalVarargs {

	@Override
	public T call(Object... parameters) {
		try {
			OperatorHelper.checkItemNotNull(parameters);
			return build(parameters);
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
