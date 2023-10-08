package com.yomahub.liteflow.exception;

/**
 * 没有找到降级组件异常
 *
 * @author DaleLee
 * @since 2.11.1
 */
public class FallbackCmpNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 异常信息
     */
    private String message;

    public FallbackCmpNotFoundException(String message) {
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
