package com.yomahub.liteflow.exception;

/**
 * node id不合法异常
 *
 * @author tangkc
 * @since 2.13.2
 */
public class NodeIdUnIllegalException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public NodeIdUnIllegalException(String message) {
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
