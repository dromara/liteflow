package com.yomahub.liteflow.exception;

/**
 * 执行异常时
 *
 * @author Yun
 */
public class WhenExecuteException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public WhenExecuteException(String message) {
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
