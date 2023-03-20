package com.yomahub.liteflow.core.proxy;

import java.lang.reflect.Method;

/**
 * @author Bryan.Zhang
 */
public class LiteFlowMethodBean {

	private String methodName;

	private Method method;

	public LiteFlowMethodBean(String methodName, Method method) {
		this.methodName = methodName;
		this.method = method;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

}
