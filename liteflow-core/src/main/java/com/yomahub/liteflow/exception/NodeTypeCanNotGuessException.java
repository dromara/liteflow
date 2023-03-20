package com.yomahub.liteflow.exception;

/**
 * 节点类型无法猜测异常
 *
 * @author Yun
 */
public class NodeTypeCanNotGuessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public NodeTypeCanNotGuessException(String message) {
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
