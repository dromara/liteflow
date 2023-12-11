package com.yomahub.liteflow.exception;

/**
 * 在非spring环境中不支持
 * @author Bryan.Zhang
 * @since 2.11.4
 */
public class NotSupportDeclException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public NotSupportDeclException(String message) {
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
