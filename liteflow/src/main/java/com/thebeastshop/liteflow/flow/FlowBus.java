/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-7-28
 * @version 1.0
 */
package com.thebeastshop.liteflow.flow;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.thebeastshop.liteflow.entity.config.Chain;
import com.thebeastshop.liteflow.entity.config.Node;

public class FlowBus {

	private static Map<String, Chain> chainMap;

	private static Map<String, Node> nodeMap;

	public static Chain getChain(String id) throws Exception{
		if(chainMap == null || chainMap.isEmpty()){
			throw new Exception("please config the rule first");
		}
		return chainMap.get(id);
	}

	public static void addChain(String name,Chain chain){
		if(chainMap == null){
			chainMap = new HashMap<>();
		}
		chainMap.put(name, chain);
	}

	public static boolean needInit() {
		return MapUtils.isEmpty(chainMap);
	}

	public static void addNode(String nodeId, Node node) {
		if(nodeMap == null) {
			nodeMap = new HashMap<>();
		}
		nodeMap.put(nodeId, node);
	}

	public static Node getNode(String nodeId) {
		return nodeMap.get(nodeId);
	}
}
