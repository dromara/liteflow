package com.yomahub.liteflow.exception;

/**
 * Json 进程异常
 *
 * @author Yun
 */
public class JsonProcessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public JsonProcessException(String message) {
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
