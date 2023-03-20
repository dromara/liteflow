package com.yomahub.liteflow.exception;

/**
 * 错误支持路径异常
 *
 * @author Yun
 */
public class ErrorSupportPathException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ErrorSupportPathException(String message) {
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
