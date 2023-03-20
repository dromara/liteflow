
package com.yomahub.liteflow.exception;

/**
 * 节点类型不支持异常
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class NodeTypeNotSupportException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public NodeTypeNotSupportException(String message) {
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
