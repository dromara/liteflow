package com.yomahub.liteflow.exception;

/**
 * 当超时异常
 *
 * @author Yun
 */
public class WhenTimeoutException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public WhenTimeoutException(String message) {
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
