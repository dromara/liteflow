
package com.yomahub.liteflow.exception;

/**
 * 循环依赖异常
 *
 * @author Yun
 */
public class CyclicDependencyException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public CyclicDependencyException(String message) {
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
