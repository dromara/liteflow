package com.yomahub.liteflow.exception;

/**
 * Chain 重复异常
 *
 * @author tangkc
 */
public class NoSuchContextBeanException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public NoSuchContextBeanException(String message) {
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
