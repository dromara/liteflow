package com.yomahub.liteflow.exception;

/**
 * 重复解析器异常
 *
 * @author Yun
 */
public class MultipleParsersException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public MultipleParsersException(String message) {
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
