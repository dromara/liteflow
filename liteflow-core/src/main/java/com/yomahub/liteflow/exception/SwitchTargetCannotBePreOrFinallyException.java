package com.yomahub.liteflow.exception;

/**
 * 切换目标不能是 Pre 或 Finally 异常
 *
 * @author Yun
 */
public class SwitchTargetCannotBePreOrFinallyException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public SwitchTargetCannotBePreOrFinallyException(String message) {
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
