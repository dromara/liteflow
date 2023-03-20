
package com.yomahub.liteflow.exception;

/**
 * 文件监听异常
 *
 * @author Bryan.Zhang
 * @since 2.10.0
 */
public class MonitorFileInitErrorException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public MonitorFileInitErrorException(String message) {
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
