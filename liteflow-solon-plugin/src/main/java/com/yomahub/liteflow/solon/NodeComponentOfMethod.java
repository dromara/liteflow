package com.yomahub.liteflow.solon;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.slot.Slot;
import org.noear.solon.core.BeanWrap;

import java.lang.reflect.Method;

/**
 * @author noear
 * @since 1.11
 */
public class NodeComponentOfMethod extends NodeComponent {

	final BeanWrap beanWrap;

	final Method method;

	final LiteFlowMethodEnum methodEnum;

	public NodeComponentOfMethod(BeanWrap beanWrap, Method method, LiteFlowMethodEnum methodEnum) {
		this.beanWrap = beanWrap;
		this.method = method;
		this.methodEnum = methodEnum;

		if (method.getParameterCount() > 1) {
			String methodFullName = beanWrap.clz().getName() + "::" + method.getName();
			throw new LiteFlowException("NodeComponent method parameter cannot be more than one: " + methodFullName);
		}

		if (method.getReturnType() != Void.class && method.getReturnType() != void.class) {
			String methodFullName = beanWrap.clz().getName() + "::" + method.getName();
			throw new LiteFlowException("NodeComponent method returnType can only be void: " + methodFullName);
		}
	}

	private void exec() throws Exception {
		if (method.getParameterCount() == 0) {
			method.invoke(beanWrap.get());
		}
		else {
			method.invoke(beanWrap.get(), this);
		}
	}

	@Override
	public void process() throws Exception {
		if (methodEnum != LiteFlowMethodEnum.PROCESS) {
			return;
		}

		exec();
	}

	@Override
	public void beforeProcess() {
		if (methodEnum != LiteFlowMethodEnum.BEFORE_PROCESS) {
			return;
		}

		try {
			exec();
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void afterProcess() {
		if (methodEnum != LiteFlowMethodEnum.AFTER_PROCESS) {
			return;
		}

		try {
			exec();
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onError(Exception e) throws Exception {
		if (methodEnum != LiteFlowMethodEnum.ON_ERROR) {
			return;
		}

		exec();
	}

	@Override
	public void onSuccess() throws Exception {
		if (methodEnum != LiteFlowMethodEnum.ON_SUCCESS) {
			return;
		}

		exec();
	}

}
