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
    }

    public static LiteflowResponse newMainResponse(Slot slot){
        return newResponse(slot, slot.getException());
    }

    public static LiteflowResponse newInnerResponse(String chainId, Slot slot){
        return newResponse(slot, slot.getSubException(chainId));
    }

    private static LiteflowResponse newResponse(Slot slot, Exception e){
        LiteflowResponse response = new LiteflowResponse();
        if (slot != null && e != null) {
            response.setSuccess(false);
            response.setCause(e);
            response.setMessage(response.getCause().getMessage());
            response.setCode(response.getCause() instanceof LiteFlowException ? ((LiteFlowException)response.getCause()).getCode() : null);
        } else {
            response.setSuccess(true);
        }
        response.setSlot(slot);
        return response;
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
