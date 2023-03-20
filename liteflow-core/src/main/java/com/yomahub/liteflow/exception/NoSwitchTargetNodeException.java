package com.yomahub.liteflow.exception;

/**
 * 无切换目标节点异常
 *
 * @author Yun
 */
public class NoSwitchTargetNodeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public NoSwitchTargetNodeException(String message) {
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
