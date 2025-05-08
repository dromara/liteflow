package com.yomahub.liteflow.exception;

/**
 * 对象转型异常
 *
 * @author Bryan.Zhang
 * @since 2.13.0
 */
public class ObjectConvertException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ObjectConvertException(String message) {
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
