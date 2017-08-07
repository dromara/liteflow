/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-8-3
 * @version 1.0
 */
package com.thebeastshop.liteflow.entity.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class Slot {
	
	private static final Logger LOG = LoggerFactory.getLogger(Slot.class);
	
	private final String REQUEST = "request";
	
	private final String RESPONSE = "response";
	
	private final String NODE_INPUT_PREFIX = "input_";
	
	private final String NODE_OUTPUT_PREFIX = "output_";
	
	private List<String> executeSteps = new ArrayList<String>();
	
	private ConcurrentHashMap<String, Object> dataMap = new ConcurrentHashMap<String, Object>();
	
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
	
	public <T> T getData(String key){
		return (T)dataMap.get(key);
	}
	
	public <T> void setData(String key, T t){
		dataMap.put(key, t);
	}
	
	public void addStep(String nodeId){
		this.executeSteps.add(nodeId);
	}
	
	public void printStep(){
		StringBuffer str = new StringBuffer();
		for(int i = 0; i < this.executeSteps.size(); i++){
			str.append(executeSteps.get(i));
			if(i < this.executeSteps.size()-1){
				str.append("==>");
			}
		}
		LOG.info(str.toString());
	}
}
