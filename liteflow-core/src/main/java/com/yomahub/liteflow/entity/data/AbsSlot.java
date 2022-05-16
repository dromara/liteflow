/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.data;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.exception.NullParamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Slot的抽象类实现
 * @author Bryan.Zhang
 * @author LeoLee
 */
@SuppressWarnings("unchecked")
public abstract class AbsSlot implements Slot {

	private static final Logger LOG = LoggerFactory.getLogger(Slot.class);

	private static final String REQUEST = "_request";

	private static final String RESPONSE = "_response";

	private static final String CHAIN_NAME = "_chain_name";

	private static final String COND_NODE_PREFIX = "_cond_";

	private static final String NODE_INPUT_PREFIX = "_input_";

	private static final String NODE_OUTPUT_PREFIX = "_output_";

	private static final String CHAIN_REQ_PREFIX = "_chain_req_";

	private static final String REQUEST_ID = "_req_id";

	private static final String EXCEPTION = "_exception";

	private static final String PRIVATE_DELIVERY_PREFIX = "_private_d_";

	private final Queue<CmpStep> executeSteps = new ConcurrentLinkedQueue<>();

	private String executeStepsStr;

	protected ConcurrentHashMap<String, Object> dataMap = new ConcurrentHashMap<String, Object>();

	private <T> void putDataMap(String key, T t) {
		if (ObjectUtil.isNull(t)) {
			//data slot is a ConcurrentHashMap, so null value will trigger NullPointerException
			throw new NullParamException("data slot can't accept null param");
		}
		dataMap.put(key, t);
	}

	public <T> T getInput(String nodeId){
		return (T)dataMap.get(NODE_INPUT_PREFIX + nodeId);
	}

	public <T> T getOutput(String nodeId){
		return (T)dataMap.get(NODE_OUTPUT_PREFIX + nodeId);
	}

	public <T> void setInput(String nodeId,T t){
		putDataMap(NODE_INPUT_PREFIX + nodeId, t);
	}

	public <T> void setOutput(String nodeId,T t){
		putDataMap(NODE_OUTPUT_PREFIX + nodeId, t);
	}

	public <T> T getRequestData(){
		return (T)dataMap.get(REQUEST);
	}

	public <T> void setRequestData(T t){
		putDataMap(REQUEST, t);
	}

	public <T> T getResponseData(){
		return (T)dataMap.get(RESPONSE);
	}

	public <T> void setResponseData(T t){
		putDataMap(RESPONSE, t);
	}

	public <T> T getChainReqData(String chainId) {
		return (T)dataMap.get(CHAIN_REQ_PREFIX + chainId);
	}

	public <T> void setChainReqData(String chainId, T t) {
		putDataMap(CHAIN_REQ_PREFIX + chainId, t);
	}

	public boolean hasData(String key){
		return dataMap.containsKey(key);
	}

	public <T> T getData(String key){
		return (T)dataMap.get(key);
	}

	public <T> void setData(String key, T t){
		putDataMap(key, t);
	}

	public <T> void setPrivateDeliveryData(String nodeId, T t){
		String privateDKey = PRIVATE_DELIVERY_PREFIX + nodeId;
		synchronized (this){
			if (dataMap.containsKey(privateDKey)){
				Queue<T> queue = (Queue<T>) dataMap.get(privateDKey);
				queue.add(t);
			}else{
				Queue<T> queue = new ConcurrentLinkedQueue<>();
				queue.add(t);
				this.setData(privateDKey, queue);
			}
		}
	}

	public <T> Queue<T> getPrivateDeliveryQueue(String nodeId){
		String privateDKey = PRIVATE_DELIVERY_PREFIX + nodeId;
		if(dataMap.containsKey(privateDKey)){
			return (Queue<T>) dataMap.get(privateDKey);
		}else{
			return null;
		}
	}

	public <T> T getPrivateDeliveryData(String nodeId){
		String privateDKey = PRIVATE_DELIVERY_PREFIX + nodeId;
		if(dataMap.containsKey(privateDKey)){
			Queue<T> queue = (Queue<T>) dataMap.get(privateDKey);
			return queue.poll();
		}else{
			return null;
		}
	}

	public <T> void setCondResult(String key, T t){
		putDataMap(COND_NODE_PREFIX + key, t);
	}

	public <T> T getCondResult(String key){
		return (T)dataMap.get(COND_NODE_PREFIX + key);
	}

	public void setChainName(String chainName) {
		putDataMap(CHAIN_NAME, chainName);
	}

	public String getChainName() {
		return (String)dataMap.get(CHAIN_NAME);
	}

	public void addStep(CmpStep step){
		this.executeSteps.add(step);
	}

	public String getExecuteStepStr(){
		StringBuilder str = new StringBuilder();
		CmpStep cmpStep;
		for (Iterator<CmpStep> it = executeSteps.iterator(); it.hasNext();) {
			cmpStep = it.next();
			str.append(cmpStep);
			if(it.hasNext()){
				str.append("==>");
			}
		}
		this.executeStepsStr = str.toString();
		return this.executeStepsStr;
	}

	public void printStep(){
		if (ObjectUtil.isNull(this.executeStepsStr)){
			this.executeStepsStr = getExecuteStepStr();
		}
		LOG.info("[{}]:CHAIN_NAME[{}]\n{}",getRequestId(),this.getChainName(), this.executeStepsStr);
	}

	@Override
	public void generateRequestId() {
		dataMap.put(REQUEST_ID, IdUtil.fastSimpleUUID());
	}

	@Override
	public String getRequestId() {
		return (String)dataMap.get(REQUEST_ID);
	}

	public Queue<CmpStep> getExecuteSteps() {
		return executeSteps;
	}

	@Override
	public Exception getException() {
		return (Exception) this.dataMap.get(EXCEPTION);
	}

	@Override
	public void setException(Exception e) {
		putDataMap(EXCEPTION, e);
	}
}
