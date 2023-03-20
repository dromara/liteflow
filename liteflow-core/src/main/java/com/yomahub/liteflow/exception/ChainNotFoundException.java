package com.yomahub.liteflow.exception;

/**
 * 链端不存在
 *
 * @author Bryan.Zhang
 */
public class ChainNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ChainNotFoundException(String message) {
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
