package com.yomahub.liteflow.exception;

/**
 * 找不到节点类异常
 *
 * @author Yun
 */
public class NodeClassNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public NodeClassNotFoundException(String message) {
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
