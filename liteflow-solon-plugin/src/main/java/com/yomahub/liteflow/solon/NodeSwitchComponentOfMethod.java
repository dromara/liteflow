package com.yomahub.liteflow.solon;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.exception.LiteFlowException;
import org.noear.solon.core.BeanWrap;

import java.lang.reflect.Method;

/**
 * @author noear
 * @since 1.11
 */
public class NodeSwitchComponentOfMethod extends NodeSwitchComponent {

	final BeanWrap beanWrap;

	final Method method;

	final LiteFlowMethodEnum methodEnum;

	public NodeSwitchComponentOfMethod(BeanWrap beanWrap, Method method, LiteFlowMethodEnum methodEnum) {
		this.beanWrap = beanWrap;
		this.method = method;
		this.methodEnum = methodEnum;

		if (method.getParameterCount() > 1) {
			String methodFullName = beanWrap.clz().getName() + "::" + method.getName();
			throw new LiteFlowException(
					"NodeSwitchComponent method parameter cannot be more than one: " + methodFullName);
		}

		if (method.getReturnType() != String.class) {
			String methodFullName = beanWrap.clz().getName() + "::" + method.getName();
			throw new LiteFlowException("NodeSwitchComponent method returnType can only be string: " + methodFullName);
		}
	}

	private Object exec() throws Exception {
		if (method.getParameterCount() == 0) {
			return method.invoke(beanWrap.get());
		}
		else {
			return method.invoke(beanWrap.get(), this);
		}
	}

	@Override
	public String processSwitch() throws Exception {
		return (String) exec();
	}

}
