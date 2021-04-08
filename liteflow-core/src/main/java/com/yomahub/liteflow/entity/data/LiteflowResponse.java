package com.yomahub.liteflow.entity.data;

import java.io.Serializable;

/**
 * 执行结果封装类
 * @author zend.wang
 */
public class LiteflowResponse<T extends Slot> implements Serializable {
    
    private static final long serialVersionUID = -2792556188993845048L;
    
    private boolean success;
    
    private String message;
    
    private Throwable cause;
    
    private T slot;
    
    public LiteflowResponse(T slot) {
        this.success = true;
        this.message = "";
        this.slot = slot;
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
    
    public T getSlot() {
        return slot;
    }
    
    public void setSlot(final T slot) {
        this.slot = slot;
    }
}
