package com.yomahub.liteflow.entity.data;

import java.io.Serializable;

/**
 * 执行结果封装类
 * @author zend.wang
 */
public class LiteflowResponse<T> implements Serializable {
    
    private static final long serialVersionUID = -2792556188993845048L;
    
    private boolean success;
    
    private String message;
    
    private Throwable cause;
    
    private T data;
    
    public LiteflowResponse(T data) {
        this.success = true;
        this.message = "";
        this.data = data;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(final boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(final String message) {
        this.message = message;
    }
    
    public Throwable getCause() {
        return cause;
    }
    
    public void setCause(final Throwable cause) {
        this.cause = cause;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(final T data) {
        this.data = data;
    }
}
