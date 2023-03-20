package com.yomahub.liteflow.exception;

/**
 * 组件不可访问异常
 *
 * @author Bryan.Zhang
 */
public class ComponentNotAccessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ComponentNotAccessException(String message) {
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
