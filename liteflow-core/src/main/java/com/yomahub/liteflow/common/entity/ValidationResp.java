package com.yomahub.liteflow.common.entity;

/**
 * 校验结果封装类
 *
 * @author gaibu
 * @since 2.12.2
 */
public class ValidationResp {

    /**
     * 是否成功，true 校验成功，false 校验失败
     */
    private boolean success;

    /**
     * 失败抛出的异常，成功时候为 null
     */
    private Exception cause;

    public ValidationResp(boolean success, Exception cause) {
        this.success = success;
        this.cause = cause;
    }

    public static ValidationResp success(){
        return new ValidationResp(true, null);
    }

    public static ValidationResp fail(Exception exception){
        return new ValidationResp(false, exception);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Exception getCause() {
        return cause;
    }

    public void setCause(Exception cause) {
        this.cause = cause;
    }
}
