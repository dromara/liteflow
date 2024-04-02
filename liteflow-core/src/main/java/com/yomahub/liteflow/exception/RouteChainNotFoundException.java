package com.yomahub.liteflow.exception;

/**
 * 决策路由没有找到异常
 *
 * @author Bryan.Zhang
 */
public class RouteChainNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public RouteChainNotFoundException(String message) {
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
