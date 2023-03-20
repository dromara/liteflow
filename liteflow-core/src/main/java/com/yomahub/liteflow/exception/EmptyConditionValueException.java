package com.yomahub.liteflow.exception;

/**
 * 空条件值异常
 *
 * @author Yun
 */
public class EmptyConditionValueException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public EmptyConditionValueException(String message) {
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
