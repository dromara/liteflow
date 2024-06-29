
package com.yomahub.liteflow.parser.nacos.exception;

public class NacosException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public NacosException(String message) {
		this.message = message;
	}

	public NacosException(Throwable cause) {
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
