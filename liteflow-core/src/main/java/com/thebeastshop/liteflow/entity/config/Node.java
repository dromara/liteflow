/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-7-28
 * @version 1.0
 */
package com.thebeastshop.liteflow.entity.config;

import java.util.HashMap;
import java.util.Map;

import com.thebeastshop.liteflow.core.NodeComponent;

public class Node {
	
	private String id;
	
	private String clazz;
	
	private NodeComponent instance;
	
	private Map<String, Node> condNodeMap = new HashMap<String, Node>();
	
	public Node(){
		
	}
	
	public Node(String id, String clazz, NodeComponent instance) {
		this.id = id;
		this.clazz = clazz;
		this.instance = instance;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	public NodeComponent getInstance() {
		return instance;
	}

	public void setInstance(NodeComponent instance) {
		this.instance = instance;
	}
	
	public Node getCondNode(String nodeId){
		return this.condNodeMap.get(nodeId);
	}
	
	public void setCondNode(String nodeId, Node condNode){
		this.condNodeMap.put(nodeId, condNode);
	}
}
