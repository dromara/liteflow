package com.yomahub.liteflow.test.script.groovy.throwException;

import com.yomahub.liteflow.exception.LiteFlowException;

public class TestException extends LiteFlowException {

	public TestException(String message) {
		super(message);
	}

	public TestException(String code, String message) {
		super(code, message);
	}

}
