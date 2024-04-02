package com.yomahub.liteflow.exception;

/**
 * 没有匹配的决策路由
 *
 * @author Bryan.Zhang
 */
public class NoMatchedRouteChainException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public NoMatchedRouteChainException(String message) {
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
