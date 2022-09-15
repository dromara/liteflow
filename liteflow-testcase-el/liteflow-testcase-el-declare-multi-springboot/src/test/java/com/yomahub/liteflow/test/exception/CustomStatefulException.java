package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.exception.LiteFlowException;

/**
 * 用户自定义带状态码的异常
 */
public class CustomStatefulException extends LiteFlowException {
	public CustomStatefulException(String code, String message) {
		super(code, message);
	}
}
