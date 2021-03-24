/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.yomahub.liteflow.entity.flow.Chain;
import com.yomahub.liteflow.entity.flow.Node;

public class FlowBus {

	private static Map<String, Chain> chainMap = new HashMap<>();

	private static Map<String, Node> nodeMap = new HashMap<>();
	
	private FlowBus() {
	}
	
	public static Chain getChain(String id) throws Exception {
		if (MapUtils.isEmpty(chainMap)) {
			throw new Exception("please config the rule first");
		}
		return chainMap.get(id);
	}

	public static void addChain(String name,Chain chain){
		chainMap.put(name, chain);
	}

	public static boolean containChain(String chainId){
		return chainMap.containsKey(chainId);
	}

	public static boolean needInit() {
		return MapUtils.isEmpty(chainMap);
	}

	public static boolean containNode(String nodeId){
		return nodeMap.containsKey(nodeId);
	}

	public static void addNode(String nodeId, Node node) {
		nodeMap.put(nodeId, node);
	}

	public static Node getNode(String nodeId) {
		return nodeMap.get(nodeId);
	}
}
