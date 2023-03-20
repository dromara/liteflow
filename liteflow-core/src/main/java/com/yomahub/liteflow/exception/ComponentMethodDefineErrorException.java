
package com.yomahub.liteflow.exception;

/**
 * 组件方法定义错误异常
 *
 * @author Bryan.Zhang
 */
public class ComponentMethodDefineErrorException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ComponentMethodDefineErrorException(String message) {
		this.message = message;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
