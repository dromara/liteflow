package com.yomahub.liteflow.exception;

/**
 * 流程执行者未初始化
 *
 * @author Yun
 */
public class FlowExecutorNotInitException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public FlowExecutorNotInitException(String message) {
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
