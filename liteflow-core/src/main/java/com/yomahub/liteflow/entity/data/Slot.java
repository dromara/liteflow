/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.data;

/**
 * Slot的接口
 * @author Bryan.Zhang
 */
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

	public String printStep();

	public void generateRequestId();

	public String getRequestId();

	public void setChainName(String chainName);

	public String getChainName();
}
