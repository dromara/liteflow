
package com.yomahub.liteflow.exception;

/**
 * @author Bryan.Zhang
 */
public class ProxyException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ProxyException(String message) {
		this.message = message;
	}

	public ProxyException(Throwable cause) {
		super(cause);
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
