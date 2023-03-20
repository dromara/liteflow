package com.yomahub.liteflow.exception;

/**
 * Chain 重复异常
 *
 * @author tangkc
 */
public class ChainDuplicateException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public ChainDuplicateException(String message) {
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
