package com.yomahub.liteflow.parser.apollo.exception;

/**
 * @author zhanghua
 * @since 2.9.5
 */
public class ApolloException extends RuntimeException {

	private String message;

	public ApolloException(String message) {
		super();
		this.message = message;
	}

	public ApolloException(Throwable cause) {
		super(cause);
		this.message = cause.getMessage();
	}

	@Override
	public String getMessage() {
		return message;
	}

}
