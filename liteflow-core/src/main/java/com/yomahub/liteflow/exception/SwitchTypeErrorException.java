package com.yomahub.liteflow.exception;

/**
 * 开关类型错误异常
 *
 * @author Yun
 */
public class SwitchTypeErrorException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public SwitchTypeErrorException(String message) {
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
