package com.yomahub.liteflow.exception;

/**
 * 没有节点异常
 *
 * @author Yun
 */
public class NoIteratorNodeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public NoIteratorNodeException(String message) {
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
