package com.yomahub.liteflow.parser.sql.exception;

/**
 * SQL 相关业务异常
 *
 * @author tangkc
 * @since 2.9.0
 */
public class ELSQLException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public ELSQLException(String message) {
		this.message = message;
	}

	public ELSQLException(Throwable cause) {
		super(cause);
		this.message = cause.getMessage();
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
