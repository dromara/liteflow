/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slot的抽象类实现
 * @author Bryan.Zhang
 */
@SuppressWarnings("unchecked")
public abstract class AbsSlot implements Slot {

	private static final Logger LOG = LoggerFactory.getLogger(Slot.class);

	private static final String REQUEST = "request";

	private static final String RESPONSE = "response";

	private static final String CHAINNAME = "chain_name";

	private static final String COND_NODE_PREFIX = "cond_";

	private static final String NODE_INPUT_PREFIX = "input_";

	private static final String NODE_OUTPUT_PREFIX = "output_";

	private static final String CHAIN_REQ_PREFIX = "chain_req_";

	private static final String REQUEST_ID = "req_id";

	private Deque<CmpStep> executeSteps = new ArrayDeque<CmpStep>();

	protected ConcurrentHashMap<String, Object> dataMap = new ConcurrentHashMap<String, Object>();

	public <T> T getInput(String nodeId){
		return (T)dataMap.get(NODE_INPUT_PREFIX + nodeId);
	}

	public <T> T getOutput(String nodeId){
		return (T)dataMap.get(NODE_OUTPUT_PREFIX + nodeId);
	}

	public <T> void setInput(String nodeId,T t){
		dataMap.put(NODE_INPUT_PREFIX + nodeId, t);
	}

	public <T> void setOutput(String nodeId,T t){
		dataMap.put(NODE_OUTPUT_PREFIX + nodeId, t);
	}

	public <T> T getRequestData(){
		return (T)dataMap.get(REQUEST);
	}

	public <T> void setRequestData(T t){
		dataMap.put(REQUEST, t);
	}

	public <T> T getResponseData(){
		return (T)dataMap.get(RESPONSE);
	}

	public <T> void setResponseData(T t){
		dataMap.put(RESPONSE, t);
	}

	public <T> T getChainReqData(String chainId) {
		return (T)dataMap.get(CHAIN_REQ_PREFIX + chainId);
	}

	public <T> void setChainReqData(String chainId, T t) {
		dataMap.put(CHAIN_REQ_PREFIX + chainId, t);
	}

	public <T> T getData(String key){
		return (T)dataMap.get(key);
	}

	public <T> void setData(String key, T t){
		dataMap.put(key, t);
	}

	public <T> void setCondResult(String key, T t){
		dataMap.put(COND_NODE_PREFIX + key, t);
	}

	public <T> T getCondResult(String key){
		return (T)dataMap.get(COND_NODE_PREFIX + key);
	}

	public void setChainName(String chainName) {
		dataMap.put(CHAINNAME, chainName);
	}

	public String getChainName() {
		return (String)dataMap.get(CHAINNAME);
	}

	public void addStep(CmpStep step){
		this.executeSteps.add(step);
	}

	public String printStep(){
		StringBuffer str = new StringBuffer();
		CmpStep cmpStep = null;
		for (Iterator<CmpStep> it = executeSteps.iterator(); it.hasNext();) {
			cmpStep = it.next();
			str.append(cmpStep);
			if(it.hasNext()){
				str.append("==>");
			}
		}
		LOG.info("[{}]:CHAIN_NAME[{}]\n{}",getRequestId(),this.getChainName(),str.toString());
		return str.toString();
	}

	@Override
	public void generateRequestId() {
		dataMap.put(REQUEST_ID, new Long(System.nanoTime()).toString());
	}

	@Override
	public String getRequestId() {
		return (String)dataMap.get(REQUEST_ID);
	}

	public Deque<CmpStep> getExecuteSteps() {
		return executeSteps;
	}
}
