package com.yomahub.liteflow.exception;

/**
 * 流程系统异常
 *
 * @author Yun
 */
public class FlowSystemException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public FlowSystemException(String message) {
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
