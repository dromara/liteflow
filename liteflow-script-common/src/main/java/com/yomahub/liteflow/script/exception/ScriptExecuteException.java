
package com.yomahub.liteflow.script.exception;

/**
 * 脚本运行异常
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class ScriptExecuteException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ScriptExecuteException(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
