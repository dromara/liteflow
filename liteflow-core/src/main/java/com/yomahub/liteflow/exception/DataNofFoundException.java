package com.yomahub.liteflow.exception;

/**
 * 未找到数据异常
 * @author tangkc
 */
public class DataNofFoundException extends RuntimeException {
    public static final String MSG = "DataNofFoundException";

    private static final long serialVersionUID = 1L;

    /**
     * 异常信息
     */
    private String message;

    public DataNofFoundException() {
        this.message = MSG;
    }

    public DataNofFoundException(String message) {
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
