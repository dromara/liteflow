package com.yomahub.liteflow.exception;

/**
 * 并行策略执行器创建异常
 *
 * @author luo yi
 * @since 2.11.0
 */
public class ParallelExecutorCreateException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ParallelExecutorCreateException(String message) {
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
