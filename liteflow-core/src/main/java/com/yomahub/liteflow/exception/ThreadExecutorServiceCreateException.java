package com.yomahub.liteflow.exception;

/**
 * 并行多线程创建异常
 *
 * @author Bryan.Zhang
 * @since 2.6.6
 */
public class ThreadExecutorServiceCreateException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ThreadExecutorServiceCreateException(String message) {
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
