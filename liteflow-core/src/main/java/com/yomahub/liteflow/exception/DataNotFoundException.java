package com.yomahub.liteflow.exception;

/**
 * 未找到数据异常
 *
 * @author tangkc
 */
public class DataNotFoundException extends RuntimeException {

	public static final String MSG = "DataNotFoundException";

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public DataNotFoundException() {
		this.message = MSG;
	}

	public DataNotFoundException(String message) {
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
