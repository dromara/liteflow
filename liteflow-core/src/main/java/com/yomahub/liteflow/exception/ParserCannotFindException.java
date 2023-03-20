package com.yomahub.liteflow.exception;

/**
 * 解析器找不到异常
 *
 * @author Yun
 */
public class ParserCannotFindException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public ParserCannotFindException(String message) {
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
