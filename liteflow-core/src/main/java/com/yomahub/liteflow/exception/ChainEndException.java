
package com.yomahub.liteflow.exception;

/**
 * 链端异常
 *
 * @author Bryan.Zhang
 */
public class ChainEndException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ChainEndException(String message) {
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
