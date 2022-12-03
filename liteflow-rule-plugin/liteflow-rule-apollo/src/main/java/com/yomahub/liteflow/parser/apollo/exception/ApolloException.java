package com.yomahub.liteflow.parser.apollo.exception;

/**
 * @Description:
 * @Author: zhanghua
 * @Date: 2022/12/3 13:45
 */
public class ApolloException extends RuntimeException {

	private String message;

	public ApolloException(String message) {
		super();
		this.message = message;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
