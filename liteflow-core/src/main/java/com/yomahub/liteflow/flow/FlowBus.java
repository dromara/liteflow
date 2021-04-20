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

import cn.hutool.core.map.MapUtil;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.flow.Chain;
import com.yomahub.liteflow.entity.flow.Node;
import com.yomahub.liteflow.exception.ComponentCannotRegisterException;
import com.yomahub.liteflow.util.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 流程元数据类
 * @author Bryan.Zhang
 */
public class FlowBus {

	private static final Logger LOG = LoggerFactory.getLogger(FlowBus.class);

	private static final Map<String, Chain> chainMap = new HashMap<>();

	private static final Map<String, Node> nodeMap = new HashMap<>();
	
	private FlowBus() {
	}
	
	public static Chain getChain(String id) throws Exception {
		if (MapUtil.isEmpty(chainMap)) {
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
		return MapUtil.isEmpty(chainMap);
	}

	public static boolean containNode(String nodeId) {
		return nodeMap.containsKey(nodeId);
	}

	public static void addNode(String nodeId, Node node) {
		nodeMap.put(nodeId, node);
	}

	public static void addNode(String nodeId, String cmpClazzStr) throws Exception{
		Class<NodeComponent> cmpClazz = (Class<NodeComponent>)Class.forName(cmpClazzStr);
		addNode(nodeId, cmpClazz);
	}

	public static void addNode(String nodeId, Class<? extends NodeComponent> cmpClazz){
		try{
			Node node = new Node();
			node.setId(nodeId);
			node.setClazz(cmpClazz.getName());
			//以node方式配置，本质上是为了适配无spring的环境，如果有spring环境，其实不用这么配置
			//这里的逻辑是判断是否能从spring上下文中取到，如果没有spring，则就是new instance了
			NodeComponent cmpInstance = SpringAware.registerOrGet(cmpClazz);
			if (ObjectUtil.isNull(cmpInstance)) {
				LOG.warn("couldn't find component class [{}] from spring context", cmpClazz.getName());
				cmpInstance = cmpClazz.newInstance();
			}
			cmpInstance.setNodeId(nodeId);
			cmpInstance.setSelf(cmpInstance);
			node.setInstance(cmpInstance);
			nodeMap.put(nodeId,node);
		}catch (Exception e){
			String error = StrUtil.format("component[{}] register error", cmpClazz.getName());
			LOG.error(error, e);
			throw new ComponentCannotRegisterException(error);
		}
	}

	public static Node getNode(String nodeId) {
		return nodeMap.get(nodeId);
	}

	public static void cleanCache(){
		chainMap.clear();
		nodeMap.clear();
	}
}
