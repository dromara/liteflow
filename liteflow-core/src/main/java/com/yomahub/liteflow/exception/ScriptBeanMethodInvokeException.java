package com.yomahub.liteflow.exception;

/**
 * ScriptBean的方法无法被调用异常
 *
 * @author Bryan.Zhang
 */
public class ScriptBeanMethodInvokeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public ScriptBeanMethodInvokeException(String message) {
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
