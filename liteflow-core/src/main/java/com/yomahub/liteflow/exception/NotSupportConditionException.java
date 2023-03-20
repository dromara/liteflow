package com.yomahub.liteflow.exception;

/**
 * 不支持条件异常
 *
 * @author Yun
 */
public class NotSupportConditionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public NotSupportConditionException(String message) {
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
