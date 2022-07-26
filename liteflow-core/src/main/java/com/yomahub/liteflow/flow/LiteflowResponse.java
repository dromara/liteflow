package com.yomahub.liteflow.flow;

import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.flow.entity.CmpStep;
import com.yomahub.liteflow.slot.Slot;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * 执行结果封装类
 * @author zend.wang
 */
public class LiteflowResponse implements Serializable {
    
    private static final long serialVersionUID = -2792556188993845048L;
    
    private boolean success;

    private String code;

    private String message;
    
    private Exception cause;
    
    private Slot slot;
    
    public LiteflowResponse() {
      this(null);
    }
    public LiteflowResponse(Slot slot) {
        if (slot != null && slot.getException() != null) {
            this.success = false;
            this.cause = slot.getException();
            this.message = this.cause.getMessage();
            this.code = this.cause instanceof LiteFlowException ? ((LiteFlowException)this.cause).getCode() : null;
        } else {
            this.success = true;
        }
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Exception getCause() {
        return cause;
    }
    
    public void setCause(final Exception cause) {
        this.cause = cause;
    }

    public Slot getSlot() {
        return slot;
    }
    
    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public <T> T getFirstContextBean(){
        return this.getSlot().getFirstContextBean();
    }

    public <T> T getContextBean(Class<T> contextBeanClazz){
        return this.getSlot().getContextBean(contextBeanClazz);
    }

    public Map<String, CmpStep> getExecuteSteps(){
        Map<String, CmpStep> map = new HashMap<>();
        this.getSlot().getExecuteSteps().forEach(cmpStep -> map.put(cmpStep.getNodeId(), cmpStep));
        return map;
    }

    public Queue<CmpStep> getExecuteStepQueue(){
        return this.getSlot().getExecuteSteps();
    }

    public String getExecuteStepStr(){
        return this.getSlot().getExecuteStepStr();
    }

    public String getExecuteStepStrWithoutTime(){
        return this.getSlot().getExecuteStepStr(false);
    }

    public String getRequestId(){
        return this.getSlot().getRequestId();
    }
}
