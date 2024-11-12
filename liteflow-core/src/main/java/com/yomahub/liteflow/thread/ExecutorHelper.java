/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.thread;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.ThreadExecutorServiceCreateException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.thread.ExecutorCondition.ExecutorCondition;
import com.yomahub.liteflow.thread.ExecutorCondition.ExecutorConditionBuilder;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 线程池工具类
 *
 * @author Bryan.Zhang
 * @author jason
 */
public class ExecutorHelper {

	private final LFLog LOG = LFLoggerManager.getLogger(ExecutorHelper.class);

	/**
	 * 此处使用Map缓存线程池信息 key - 线程池构建者的Class全类名 value - 线程池对象
	 */
	private final Map<String, ExecutorService> executorServiceMap;

	private ExecutorHelper() {
		executorServiceMap = MapUtil.newConcurrentHashMap();
	}

	/**
	 * 使用静态内部类实现单例模式
	 */
	private static class Holder {

		static final ExecutorHelper INSTANCE = new ExecutorHelper();

	}

	public static ExecutorHelper loadInstance() {
		return Holder.INSTANCE;
	}

	/**
	 *
	 * <p>
	 * @param pool 需要关闭的线程组.
	 */
	public void shutdownAwaitTermination(ExecutorService pool) {
		shutdownAwaitTermination(pool, 60L);
	}

	/**
	 * <p>
	 * 关闭ExecutorService的线程管理者
	 * <p>
	 * @param pool 需要关闭的管理者
	 * @param timeout 等待时间
	 */
	public void shutdownAwaitTermination(ExecutorService pool, long timeout) {
		pool.shutdown();
		try {
			if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
				pool.shutdownNow();
				if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
					LOG.error("Pool did not terminate.");
				}
			}
		}
		catch (InterruptedException ie) {
			pool.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	// 构建默认when线程池
	public ExecutorService buildWhenExecutor() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		return buildWhenExecutor(liteflowConfig.getGlobalThreadPoolExecutorClass());
	}

	// 构建when线程池 - 支持多个when公用一个线程池
	public ExecutorService buildWhenExecutor(String clazz) {
		if (StrUtil.isBlank(clazz)) {
			return buildWhenExecutor();
		}
		return getExecutorService(clazz);
	}

	// 构建when线程池 - clazz和condition的hash值共同作为缓存key
	public ExecutorService buildWhenExecutorWithHash(String hash) {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		return buildWhenExecutorWithHash(liteflowConfig.getGlobalThreadPoolExecutorClass(), hash);
	}

	// 构建when线程池 - clazz和condition的hash值共同作为缓存key
	public ExecutorService buildWhenExecutorWithHash(String clazz, String hash) {
		if (StrUtil.isBlank(clazz)) {
			return buildWhenExecutorWithHash(hash);
		}
		return getExecutorService(clazz, hash);
	}

	// 构建默认的FlowExecutor线程池，用于execute2Future方法
	public ExecutorService buildMainExecutor() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		return buildMainExecutor(liteflowConfig.getMainExecutorClass());
	}

	public ExecutorService buildMainExecutor(String clazz) {
		if (StrUtil.isBlank(clazz)) {
			return buildMainExecutor();
		}
		return getExecutorService(clazz);
	}

	private ExecutorService getExecutorService(String clazz){
		return getExecutorService(clazz, null);
	}

	/**
	 * 根据线程执行构建者Class类名获取ExecutorService实例
	 */
	private ExecutorService getExecutorService(String clazz, String hash) {
		try {
			String key;
			if (StrUtil.isBlank(hash)) {
				key = clazz;
			}else{
				key = StrUtil.format("{}_{}", clazz, hash);
			}

			ExecutorService executorServiceFromCache = executorServiceMap.get(key);
			if (ObjectUtil.isNotNull(executorServiceFromCache)) {
				return executorServiceFromCache;
			}
			else {
				Class<ExecutorBuilder> executorClass = (Class<ExecutorBuilder>) Class.forName(clazz);
				ExecutorBuilder executorBuilder = ContextAwareHolder.loadContextAware().registerBean(executorClass);
				ExecutorService executorService = executorBuilder.buildExecutor();
				executorServiceMap.put(key, executorService);
				return executorService;
			}
		}
		catch (Exception e) {
			LOG.error(e.getMessage());
			throw new ThreadExecutorServiceCreateException(e.getMessage());
		}
	}

	public void clearExecutorServiceMap() {
		if (MapUtil.isNotEmpty(executorServiceMap)) {
			executorServiceMap.clear();
		}
	}

	/**
	 * 构建执行器服务
	 *
	 * @param condition 条件对象（Loop或When条件）
	 * @param slotIndex 槽索引
	 * @param type      condition类型
	 * @return ExecutorService
	 */
	public ExecutorService buildExecutorService(Condition condition, Integer slotIndex, ConditionTypeEnum type) {
		ExecutorService executor;
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		String chainId = DataBus.getSlot(slotIndex).getChainId();
		Chain chain = FlowBus.getChain(chainId);

		// 构建条件判断对象
		ExecutorCondition execCondition = ExecutorConditionBuilder.buildExecutorCondition(
				condition,
				chain,
				liteflowConfig,
				type
		);

		// 根据条件选择执行器
		if (execCondition.isConditionLevel()) {
			// condition层级线程池
			executor = getExecutorService(execCondition.getConditionExecutorClass(),
										  String.valueOf(condition.hashCode()));

		} else if (execCondition.isChainLevel()) {
			// chain层级线程池
			executor = getExecutorService(chain.getThreadPoolExecutorClass(),
										  String.valueOf(chain.hashCode()));
		} else {
			// 全局线程池
			executor = getExecutorService(liteflowConfig.getGlobalThreadPoolExecutorClass());
		}

		return executor;
	}


}
