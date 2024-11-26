package com.yomahub.liteflow.flow.executor;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

import java.util.Map;

/**
 * 节点执行器帮助器
 *
 * @author sikadai
 * @since 2.6.9
 */
public class NodeExecutorHelper {

	/**
	 * 此处使用Map缓存线程池信息
	 */
	private final Map<Class<? extends NodeExecutor>, NodeExecutor> nodeExecutorMap;

	private NodeExecutorHelper() {
		nodeExecutorMap = MapUtil.newConcurrentHashMap();
	}

	/**
	 * 使用静态内部类实现单例模式
	 */
	private static class Holder {

		static final NodeExecutorHelper INSTANCE = new NodeExecutorHelper();

	}

	/**
	 * 获取帮助者的实例
	 * @return NodeExecutorHelper
	 */
	public static NodeExecutorHelper loadInstance() {
		// 外围类能直接访问内部类（不管是否是静态的）的私有变量
		return Holder.INSTANCE;
	}

	public NodeExecutor buildNodeExecutor(Class<? extends NodeExecutor> nodeExecutorClass) {
		if (nodeExecutorClass == null) {
			// 此处使用默认的节点执行器进行执行
			nodeExecutorClass = DefaultNodeExecutor.class;
		}
		NodeExecutor nodeExecutor = nodeExecutorMap.get(nodeExecutorClass);
		// 此处无需使用同步锁进行同步-因为即使同时创建了两个实例，但是添加到缓存中的只会存在一个且不会存在并发问题-具体是由ConcurrentMap保证
		if (ObjectUtil.isNull(nodeExecutor)) {
			// 获取重试执行器实例
			nodeExecutor = ContextAwareHolder.loadContextAware().registerBean(nodeExecutorClass);
			// 缓存
			nodeExecutorMap.put(nodeExecutorClass, nodeExecutor);
		}
		return nodeExecutorMap.get(nodeExecutorClass);
	}

}
