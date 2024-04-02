package com.yomahub.liteflow.exception;

/**
 * Route语句不符合规范异常
 *
 * @author Bryan.Zhang
 */
public class RouteELInvalidException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public RouteELInvalidException(String message) {
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
