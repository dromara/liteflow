package com.yomahub.liteflow.exception;

/**
 * 执行未完全实现的抽象Chain
 *
 * @author zy
 * @since 2.11.1
 */

public class ChainNotImplementedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * 异常信息
     */
    private String message;

    public ChainNotImplementedException(String message) {
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
