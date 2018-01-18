/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-12-4
 * @version 1.0
 */
package com.thebeastshop.liteflow.entity.data;

public interface Slot {
	public <T> T getInput(String nodeId);
	
	public <T> T getOutput(String nodeId);
	
	public <T> void setInput(String nodeId,T t);
	
	public <T> void setOutput(String nodeId,T t);
	
	public <T> T getRequestData();
	
	public <T> void setRequestData(T t);
	
	public <T> T getResponseData();
	
	public <T> void setChainReqData(String chainId, T t);
	
	public <T> T getChainReqData(String chainId);
	
	public <T> void setResponseData(T t);
	
	public <T> T getData(String key);
	
	public <T> void setData(String key, T t);
	
	public <T> void setCondResult(String key, T t);
	
	public <T> T getCondResult(String key);
	
	public void addStep(CmpStep step);
	
	public void printStep();
	
	public void generateRequestId();
	
	public String getRequestId();
	
	public void setChainName(String chainName);
	
	public String getChainName();
}
