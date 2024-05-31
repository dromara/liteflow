package com.yomahub.liteflow.exception;

/**
 * @author Bryan.Zhang
 */
public class ParameterFactException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ParameterFactException(String message) {
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
