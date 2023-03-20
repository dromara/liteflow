package com.yomahub.liteflow.exception;

import java.io.Serializable;

/**
 * null param exception when param is null, dataMap (ConcurrentHashMap) cann't accept a
 * null value
 *
 * @author LeoLee
 * @since 2.6.6
 */
public class NullParamException extends RuntimeException implements Serializable {

	private static final long serialVersionUID = -864259139568071245L;

	private String message;

	public NullParamException(String message) {
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
