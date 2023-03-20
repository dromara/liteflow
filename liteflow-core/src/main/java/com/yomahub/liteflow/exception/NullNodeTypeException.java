package com.yomahub.liteflow.exception;

/**
 * 空节点异常
 *
 * @author Yun
 */
public class NullNodeTypeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public NullNodeTypeException(String message) {
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
