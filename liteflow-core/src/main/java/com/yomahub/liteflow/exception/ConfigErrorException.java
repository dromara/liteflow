package com.yomahub.liteflow.exception;

/**
 * 配置错误异常
 *
 * @author Yun
 */
public class ConfigErrorException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ConfigErrorException(String message) {
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
