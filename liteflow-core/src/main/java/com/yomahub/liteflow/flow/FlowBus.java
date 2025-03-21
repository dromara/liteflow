/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.util.AnnoUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.ComponentInitializer;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.ScriptComponent;
import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.enums.ParseModeEnum;
import com.yomahub.liteflow.exception.ComponentCannotRegisterException;
import com.yomahub.liteflow.exception.NullNodeTypeException;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.lifecycle.LifeCycleHolder;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;
import com.yomahub.liteflow.parser.el.LocalJsonFlowELParser;
import com.yomahub.liteflow.parser.el.LocalXmlFlowELParser;
import com.yomahub.liteflow.parser.el.LocalYmlFlowELParser;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.script.ScriptExecutorFactory;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.script.exception.ScriptSpiException;
import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.spi.holder.DeclComponentParserHolder;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 流程元数据类
 *
 * @author Bryan.Zhang
 * @author DaleLee
 * @author Jay li
 */
public class FlowBus {

	private static final LFLog LOG = LFLoggerManager.getLogger(FlowBus.class);

	private static final Map<String, Chain> chainMap;

	private static final Map<String, Node> nodeMap;

	private static final Map<String, Node> fallbackNodeMap;

	private static final AtomicBoolean initStat = new AtomicBoolean(false);

	static {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		if (liteflowConfig.getFastLoad()){
			chainMap = new HashMap<>();
			nodeMap = new HashMap<>();
			fallbackNodeMap = new HashMap<>();
		}else{
			chainMap = new CopyOnWriteHashMap<>();
			nodeMap = new CopyOnWriteHashMap<>();
			fallbackNodeMap = new CopyOnWriteHashMap<>();
		}
	}

	public static Chain getChain(String id) {
		return chainMap.get(id);
	}

	// 这一方法主要用于第一阶段chain的预装载
	public static void addChain(String chainName) {
		if (!chainMap.containsKey(chainName)) {
			chainMap.put(chainName, new Chain(chainName));
		}
	}

	// 这个方法主要用于第二阶段的替换chain
	public static void addChain(Chain chain) {
		//如果有生命周期则执行相应生命周期实现
		if (CollUtil.isNotEmpty(LifeCycleHolder.getPostProcessChainBuildLifeCycleList())){
			LifeCycleHolder.getPostProcessChainBuildLifeCycleList().forEach(
					postProcessAfterChainBuildLifeCycle -> postProcessAfterChainBuildLifeCycle.postProcessBeforeChainBuild(chain)
			);
		}

		chainMap.put(chain.getChainId(), chain);

		//如果有生命周期则执行相应生命周期实现
		if (CollUtil.isNotEmpty(LifeCycleHolder.getPostProcessChainBuildLifeCycleList())){
			LifeCycleHolder.getPostProcessChainBuildLifeCycleList().forEach(
					postProcessAfterChainBuildLifeCycle -> postProcessAfterChainBuildLifeCycle.postProcessAfterChainBuild(chain)
			);
		}
	}

	public static boolean containChain(String chainId) {
		return chainMap.containsKey(chainId);
	}

	public static boolean needInit() {
		return initStat.compareAndSet(false, true);
	}

	public static boolean containNode(String nodeId) {
		return nodeMap.containsKey(nodeId);
	}

	public static void addManagedNode(String nodeId) {
		ContextAware contextAware = ContextAwareHolder.loadContextAware();
		if (contextAware.hasBean(nodeId)){
			addManagedNode(nodeId, contextAware.getBean(nodeId));
		}
	}

	/**
	 * 添加已托管的节点（如：Spring、Solon 管理的节点）
	 * @param nodeId nodeId
	 * @param nodeComponent nodeComponent
	 */
	public static void addManagedNode(String nodeId, NodeComponent nodeComponent) {
		// 根据class来猜测类型
		NodeTypeEnum type = NodeTypeEnum.guessType(nodeComponent.getClass());

		if (type == null) {
			throw new NullNodeTypeException(StrUtil.format("node type is null for node[{}]", nodeId));
		}

		Node node = new Node(ComponentInitializer.loadInstance()
				.initComponent(nodeComponent, type, nodeComponent.getName(), nodeId));
		put2NodeMap(nodeId, node);
		addFallbackNode(node);
	}

	/**
	 * 添加 node
	 * @param nodeId 节点id
	 * @param name 节点名称
	 * @param type 节点类型
	 * @param cmpClazz 节点组件类
	 */
	public static void addNode(String nodeId, String name, NodeTypeEnum type, Class<?> cmpClazz) {
		addNode(nodeId, name, type, cmpClazz, null, null);
	}

	/**
	 * 添加 node
	 * @param nodeId 节点id
	 * @param name 节点名称
	 * @param nodeType 节点类型
	 * @param cmpClazzStr 节点组件类路径
	 */
	public static void addNode(String nodeId, String name, NodeTypeEnum nodeType, String cmpClazzStr) {
		Class<?> cmpClazz;
		try {
			cmpClazz = Class.forName(cmpClazzStr);
		}
		catch (Exception e) {
			throw new ComponentCannotRegisterException(e.getMessage());
		}
		addNode(nodeId, name, nodeType, cmpClazz, null, null);
	}

	/**
	 * 添加脚本 node
	 * @param nodeId 节点id
	 * @param name 节点名称
	 * @param nodeType 节点类型
	 * @param script 脚本
	 * @param language 语言
	 */
	public static void addScriptNode(String nodeId, String name, NodeTypeEnum nodeType, String script,
			String language) {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

		// 如果是PARSE_ONE_ON_FIRST_EXEC模式，则不进行脚本加载，而是直接把脚本内容放到node中
        if (liteflowConfig.getParseMode().equals(ParseModeEnum.PARSE_ONE_ON_FIRST_EXEC)) {
			List<Node> nodes = LiteflowMetaOperator.getNodesInAllChain(nodeId);
			if (CollectionUtil.isNotEmpty(nodes)) {
				nodes.forEach(node -> {
                    node.setCompiled(false);
                    node.setScript(script);
                });
			}

			Node node = new Node(nodeId, name, nodeType, script, language);
			nodeMap.put(nodeId, node);
		} else {
			addScriptNodeAndCompile(nodeId, name, nodeType, script, language);
        }
    }


	/**
	 * 添加脚本 node，并且编译脚本
	 * @param nodeId nodeId
	 * @param name name
	 * @param type type
	 * @param script script content
	 * @param language language
	 * @return NodeComponent instance
	 */
	public static NodeComponent addScriptNodeAndCompile(String nodeId, String name, NodeTypeEnum type, String script,
		String language) {
		addNode(nodeId, name, type, ScriptComponent.ScriptComponentClassMap.get(type), script, language);
		return nodeMap.get(nodeId).getInstance();
	}


	private static void addNode(String nodeId, String name, NodeTypeEnum type, Class<?> cmpClazz, String script,
			String language) {
		try {
			// 判断此类是否是声明式的组件，如果是声明式的组件，就用动态代理生成实例
			// 如果不是声明式的，就用传统的方式进行判断
			List<NodeComponent> cmpInstanceList = new ArrayList<>();
			if (LiteFlowProxyUtil.isDeclareCmp(cmpClazz)) {
				// 如果是spring体系，把原始的类往spring上下文中进行注册，那么会走到ComponentScanner中
				// 由于ComponentScanner中已经对原始类进行了动态代理，出来的对象已经变成了动态代理类，所以这时候的bean已经是NodeComponent的子类了
				// 所以spring体系下，无需再对这个bean做二次代理
				// 非spring体系下，从2.11.4开始不再支持声明式组件
				List<DeclWarpBean> declWarpBeanList = DeclComponentParserHolder.loadDeclComponentParser().parseDeclBean(cmpClazz, nodeId, name);

				cmpInstanceList = declWarpBeanList.stream().map(
						declWarpBean -> (NodeComponent)ContextAwareHolder.loadContextAware().registerDeclWrapBean(nodeId, declWarpBean)
				).collect(Collectors.toList());
			}
			else {
				// 以node方式配置，本质上是为了适配无spring的环境，如果有spring环境，其实不用这么配置
				// 这里的逻辑是判断是否能从spring上下文中取到，如果没有spring，则就是new instance了
				// 如果是script类型的节点，因为class只有一个，所以也不能注册进spring上下文，注册的时候需要new Instance
				if (!type.isScript()) {
					cmpInstanceList = ListUtil
							.toList((NodeComponent) ContextAwareHolder.loadContextAware().registerOrGet(nodeId, cmpClazz));
				}
				// 如果为空
				if (cmpInstanceList.isEmpty()) {
					NodeComponent cmpInstance = (NodeComponent) cmpClazz.newInstance();
					cmpInstanceList.add(cmpInstance);
				}
			}
			// 进行初始化component
			cmpInstanceList = cmpInstanceList.stream()
					.map(cmpInstance -> ComponentInitializer.loadInstance()
							.initComponent(cmpInstance, type, name,
									cmpInstance.getNodeId() == null ? nodeId : cmpInstance.getNodeId()))
					.collect(Collectors.toList());


			// 初始化Node，把component放到Node里去
			List<Node> nodes = cmpInstanceList.stream().map(Node::new).collect(Collectors.toList());

			for (int i = 0; i < nodes.size(); i++) {
				Node node = nodes.get(i);
				NodeComponent cmpInstance = cmpInstanceList.get(i);
				// 如果是脚本节点，则还要加载script脚本
				if (type.isScript()) {
					if (StrUtil.isNotBlank(script)) {
						node.setScript(script);
						node.setLanguage(language);
						((ScriptComponent) cmpInstance).loadScript(script, language);
					}
					else {
						String errorMsg = StrUtil.format("script for node[{}] is empty", nodeId);
						throw new ScriptLoadException(errorMsg);
					}
				}
				String activeNodeId = StrUtil.isEmpty(cmpInstance.getNodeId()) ? nodeId : cmpInstance.getNodeId();
				put2NodeMap(activeNodeId, node);
				addFallbackNode(node);
			}
		}
		catch (Exception e) {
			String error = StrUtil.format("component[{}] register error",
					StrUtil.isEmpty(name) ? nodeId : StrUtil.format("{}({})", nodeId, name));
			LOG.error(e.getMessage());
			throw new ComponentCannotRegisterException(StrUtil.format("{} {}", error, e.getMessage()));
		}
	}

	public static Node getNode(String nodeId) {
		return nodeMap.get(nodeId);
	}

	public static Map<String, Node> getNodeMap() {
		return nodeMap;
	}

	public static Map<String, Chain> getChainMap() {
		return chainMap;
	}

	public static Node getFallBackNode(NodeTypeEnum nodeType){
		String key = StrUtil.format("FB_{}", nodeType.name());
		return fallbackNodeMap.get(key);
	}

	public static void cleanCache() {
		chainMap.clear();
		nodeMap.clear();
		fallbackNodeMap.clear();
		cleanScriptCache();
	}

	public static void cleanScriptCache() {
		// 如果引入了脚本组件SPI，则还需要清理脚本的缓存
		try {
			ScriptExecutorFactory.loadInstance().cleanScriptCache();
		}
		catch (ScriptSpiException ignored) {
		}
	}

	public static void refreshFlowMetaData(FlowParserTypeEnum type, String content) throws Exception {
		if (type.equals(FlowParserTypeEnum.TYPE_EL_XML)) {
			new LocalXmlFlowELParser().parse(content);
		}
		else if (type.equals(FlowParserTypeEnum.TYPE_EL_JSON)) {
			new LocalJsonFlowELParser().parse(content);
		}
		else if (type.equals(FlowParserTypeEnum.TYPE_EL_YML)) {
			new LocalYmlFlowELParser().parse(content);
		}
	}

	public static boolean removeChain(String chainId) {
		if (containChain(chainId)) {
			chainMap.remove(chainId);
			return true;
		}
		else {
			String errMsg = StrUtil.format("cannot find the chain[{}]", chainId);
			LOG.error(errMsg);
			return false;
		}
	}

	public static void removeChain(String... chainIds) {
		Arrays.stream(chainIds).forEach(FlowBus::removeChain);
	}

	// 移除节点
	public static boolean removeNode(String nodeId) {
		return nodeMap.remove(nodeId) != null;
	}

	// 判断是否是降级组件，如果是则添加到 fallbackNodeMap
	private static void addFallbackNode(Node node) {
		NodeComponent nodeComponent = node.getInstance();
		FallbackCmp fallbackCmp = AnnoUtil.getAnnotation(nodeComponent.getClass(), FallbackCmp.class);
		if (fallbackCmp == null) {
			return;
		}

		NodeTypeEnum nodeType = node.getType();
		String key = StrUtil.format("FB_{}", nodeType.name());
		fallbackNodeMap.put(key, node);
	}

    // 重新加载脚本
	public static void reloadScript(String nodeId, String script) {
		Node node = getNode(nodeId);
		if (node == null || !node.getType().isScript()) {
			return;
		}
        // 更新元数据模版中的脚本
		node.setScript(script);

		// 更新Chain中的Node中的脚本
		LiteflowMetaOperator.getNodesInAllChain(nodeId).forEach(n -> n.setScript(script));

		ScriptExecutorFactory.loadInstance()
				.getScriptExecutor(node.getLanguage())
				.load(nodeId, script);
	}

	// 卸载脚本节点
	public static boolean unloadScriptNode(String nodeId) {
		Node node = getNode(nodeId);
		if (node == null || !node.getType().isScript()) {
			return false;
		}
		// 卸载脚本
		ScriptExecutorFactory.loadInstance()
				.getScriptExecutor(node.getLanguage())
				.unLoad(nodeId);
		// 移除脚本
		return removeNode(nodeId);
	}

	// 重新加载规则
	public static void reloadChain(String chainId, String elContent) {
		reloadChain(chainId, elContent, null);
	}

	public static void reloadChain(String chainId, String elContent, String routeContent) {
		LiteFlowChainELBuilder.createChain().setChainId(chainId).setEL(elContent).setRoute(routeContent).build();
	}

	public static void clearStat(){
		initStat.set(false);
	}

	private static void put2NodeMap(String nodeId, Node node){
		// 如果有生命周期则执行相应生命周期实现
		if (CollUtil.isNotEmpty(LifeCycleHolder.getPostProcessNodeBuildLifeCycleList())){
			LifeCycleHolder.getPostProcessNodeBuildLifeCycleList().forEach(
					postProcessAfterNodeBuildLifeCycle -> postProcessAfterNodeBuildLifeCycle.postProcessBeforeNodeBuild(node)
			);
		}

		nodeMap.put(nodeId, node);

		// 如果有生命周期则执行相应生命周期实现
		if (CollUtil.isNotEmpty(LifeCycleHolder.getPostProcessNodeBuildLifeCycleList())){
			LifeCycleHolder.getPostProcessNodeBuildLifeCycleList().forEach(
					postProcessAfterNodeBuildLifeCycle -> postProcessAfterNodeBuildLifeCycle.postProcessAfterNodeBuild(node)
			);
		}
	}

}
