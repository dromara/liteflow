
package com.yomahub.liteflow.parser.zk.exception;

public class ZkException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ZkException(String message) {
		this.message = message;
	}

	public ZkException(Throwable cause) {
		super(cause);
		this.message = cause.getMessage();
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
