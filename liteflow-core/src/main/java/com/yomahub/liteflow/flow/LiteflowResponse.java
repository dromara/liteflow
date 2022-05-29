package com.yomahub.liteflow.flow;

import com.yomahub.liteflow.flow.entity.CmpStep;
import com.yomahub.liteflow.slot.Slot;

import java.io.Serializable;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 执行结果封装类
 * @author zend.wang
 */
public class LiteflowResponse<T> implements Serializable {
    
    private static final long serialVersionUID = -2792556188993845048L;
    
    private boolean success;
    
    private String message;
    
    private Throwable cause;
    
    private Slot<T> slot;
    
    public LiteflowResponse() {
      this(null);
    }
    public LiteflowResponse(Slot<T> slot) {
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
    
    public Slot<T> getSlot() {
        return slot;
    }
    
    public void setSlot(Slot<T> slot) {
        this.slot = slot;
    }

    public T getContextBean(){
        return getSlot().getContextBean();
    }

    public Map<String, CmpStep> getExecuteSteps(){
        return getSlot().getExecuteSteps().stream().collect(Collectors.toMap(CmpStep::getNodeId, cmpStep -> cmpStep));
    }

    public String getExecuteStepStr(){
        return getSlot().getExecuteStepStr();
    }
}
