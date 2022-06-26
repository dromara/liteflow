package com.yomahub.liteflow.exception;

/**
 * RequestIdGenerator 构建异常
 *
 * @author tangkc
 */
public class RequestIdGeneratorException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public RequestIdGeneratorException(String message) {
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
