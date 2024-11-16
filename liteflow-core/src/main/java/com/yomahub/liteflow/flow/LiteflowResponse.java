package com.yomahub.liteflow.flow;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.flow.entity.CmpStep;
import com.yomahub.liteflow.slot.Slot;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

/**
 * 执行结果封装类
 *
 * @author zend.wang
 */
public class LiteflowResponse {

	private String chainId;

	private boolean success;

	private String code;

	private String message;

	private Exception cause;

	private Slot slot;

	public LiteflowResponse() {
	}

	public static LiteflowResponse newMainResponse(Slot slot) {
		return newResponse(slot, slot.getException());
	}

	public static LiteflowResponse newInnerResponse(String chainId, Slot slot) {
		return newResponse(slot, slot.getSubException(chainId));
	}

	private static LiteflowResponse newResponse(Slot slot, Exception e) {
		LiteflowResponse response = new LiteflowResponse();
		response.setChainId(slot.getChainId());
		if (e != null) {
			response.setSuccess(false);
			response.setCause(e);
			response.setMessage(response.getCause().getMessage());
			response.setCode(response.getCause() instanceof LiteFlowException
					? ((LiteFlowException) response.getCause()).getCode() : null);
		}
		else {
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

	public <T> T getFirstContextBean() {
		return this.getSlot().getFirstContextBean();
	}

	public <T> T getContextBean(Class<T> contextBeanClazz) {
		return this.getSlot().getContextBean(contextBeanClazz);
	}

	public <T> T getContextBean(String contextName) {
		return this.getSlot().getContextBean(contextName);
	}

	public Map<String, List<CmpStep>> getExecuteSteps() {
		Map<String, List<CmpStep>> map = new LinkedHashMap<>();
		this.getSlot().getExecuteSteps().forEach(cmpStep -> {
			if (map.containsKey(cmpStep.getNodeId())){
				map.get(cmpStep.getNodeId()).add(cmpStep);
			}else{
				map.put(cmpStep.getNodeId(), ListUtil.toList(cmpStep));
			}
		});
		return map;
	}

	public Queue<CmpStep> getRollbackStepQueue() {
		return this.getSlot().getRollbackSteps();
	}

	public String getRollbackStepStr() {
		return getRollbackStepStrWithoutTime();
	}

	public String getRollbackStepStrWithTime() {
		return this.getSlot().getRollbackStepStr(true);
	}

	public String getRollbackStepStrWithoutTime() {
		return this.getSlot().getRollbackStepStr(false);
	}

	public Map<String, List<CmpStep>> getRollbackSteps() {
		Map<String, List<CmpStep>> map = new LinkedHashMap<>();
		this.getSlot().getRollbackSteps().forEach(cmpStep -> {
			if (map.containsKey(cmpStep.getNodeId())){
				map.get(cmpStep.getNodeId()).add(cmpStep);
			}else{
				map.put(cmpStep.getNodeId(), ListUtil.toList(cmpStep));
			}
		});
		return map;
	}

	public Queue<CmpStep> getExecuteStepQueue() {
		return this.getSlot().getExecuteSteps();
	}

	public String getExecuteStepStr() {
		return getExecuteStepStrWithoutTime();
	}

	public String getExecuteStepStrWithInstanceId() {
		return this.getSlot().getExecuteStepStrWithInstanceId();
	}

	public String getExecuteStepStrWithTime() {
		return this.getSlot().getExecuteStepStr(true);
	}

	public String getExecuteStepStrWithoutTime() {
		return this.getSlot().getExecuteStepStr(false);
	}

	public String getRequestId() {
		return this.getSlot().getRequestId();
	}

	public String getChainId() {
		return chainId;
	}

	public void setChainId(String chainId) {
		this.chainId = chainId;
	}

	public List<String> getTimeoutItems(){
		return slot.getTimeoutItemList();
	}
}
