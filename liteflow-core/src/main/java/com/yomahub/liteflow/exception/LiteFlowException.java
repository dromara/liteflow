
package com.yomahub.liteflow.exception;

/**
 * LiteFlow架内部逻辑发生错误抛出的异常 (自定义此异常方便开发者在做全局异常处理时分辨异常类型)
 *
 * @author zendwang
 * @since 2.8.3
 */
public class LiteFlowException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常状态码 */
	private String code;

	/**
	 * 构建一个异常
	 * @param message 异常描述信息
	 */
	public LiteFlowException(String message) {
		super(message);
	}

	/**
	 * 构建一个异常
	 * @param code 异常状态码
	 * @param message 异常描述信息
	 */
	public LiteFlowException(String code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * 构建一个异常
	 * @param cause 异常对象
	 */
	public LiteFlowException(Throwable cause) {
		super(cause);
	}

	/**
	 * 构建一个异常
	 * @param message 异常信息
	 * @param cause 异常对象
	 */
	public LiteFlowException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 构建一个异常
	 * @param code 异常状态码
	 * @param message 异常信息
	 * @param cause 异常对象
	 */
	public LiteFlowException(String code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * @return 获得异常状态码
	 */
	public String getCode() {
		return code;
	}

}
