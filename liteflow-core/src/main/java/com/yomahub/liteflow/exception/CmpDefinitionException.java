package com.yomahub.liteflow.exception;

/**
 * 组件定义异常
 * @author Bryan.Zhang
 * @since 2.11.4
 */
public class CmpDefinitionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public CmpDefinitionException(String message) {
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
