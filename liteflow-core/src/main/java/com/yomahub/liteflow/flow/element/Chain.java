/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow.element;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.IdUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.common.ChainConstant;
import com.yomahub.liteflow.enums.ExecuteableTypeEnum;
import com.yomahub.liteflow.exception.ChainEndException;
import com.yomahub.liteflow.exception.FlowSystemException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.lifecycle.LifeCycleHolder;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * chain对象，实现可执行器
 *
 * @author Bryan.Zhang
 * @author jason
 * @author DaleLee
 */
public class Chain implements Executable{

	private static final LFLog LOG = LFLoggerManager.getLogger(Chain.class);

	private String chainId;

	private Executable routeItem;

	private List<Condition> conditionList = new ArrayList<>();

	private String el;

	private boolean isCompiled = true;

	private String namespace = ChainConstant.DEFAULT_NAMESPACE;

    private String threadPoolExecutorClass;

	public Chain(String chainName) {
		this.chainId = chainName;
	}

	public Chain() {
	}

	public Chain(String chainName, List<Condition> conditionList) {
		this.chainId = chainName;
		this.conditionList = conditionList;
	}

	public List<Condition> getConditionList() {
		return conditionList;
	}

	public void setConditionList(List<Condition> conditionList) {
		this.conditionList = conditionList;
	}

	/**
	 * @deprecated 请使用{@link #getChainId()}
	 * @return chainId
	 */
	@Deprecated
	public String getChainName() {
		return chainId;
	}

	/**
	 * @param chainName chainId
	 * @deprecated 请使用 {@link #setChainId(String)}
	 */
	@Deprecated
	public void setChainName(String chainName) {
		this.chainId = chainName;
	}

	public String getChainId() {
		return chainId;
	}

	public void setChainId(String chainId) {
		this.chainId = chainId;
	}

	// 执行chain的主方法
	@Override
	public void execute(Integer slotIndex) throws Exception {
		if (BooleanUtil.isFalse(isCompiled)) {
			LiteFlowChainELBuilder.buildUnCompileChain(this);
		}

		// 这里先拿到this.conditionList的引用
		// 因为在正式执行condition之前，this.conditionList有可能被其他线程置空
		// 比如，该chain在规则缓存中被淘汰
		List<Condition> conditionListRef = this.conditionList;
		// 但在编译后到拿到引用之前，this.conditionList也有可能被置空
		if (CollUtil.isEmpty(conditionListRef)) {
			// 如果conditionListRef为空，
			// 构建临时conditionList确保本次一定可以执行
			conditionListRef = buildTemporaryConditionList();
		}
		Slot slot = DataBus.getSlot(slotIndex);
		try {
			//如果有生命周期则执行相应生命周期实现
			if (CollUtil.isNotEmpty(LifeCycleHolder.getPostProcessChainExecuteLifeCycleList())){
				LifeCycleHolder.getPostProcessChainExecuteLifeCycleList().forEach(
						postProcessChainExecuteLifeCycle -> postProcessChainExecuteLifeCycle.postProcessBeforeChainExecute(chainId, slot)
				);
			}

			// 设置主ChainName
			slot.setChainId(chainId);
			// 执行主体Condition
			for (Condition condition : conditionListRef) {
				condition.setCurrChainId(chainId);
				condition.execute(slotIndex);
			}

			//如果有生命周期则执行相应生命周期实现
			if (CollUtil.isNotEmpty(LifeCycleHolder.getPostProcessChainExecuteLifeCycleList())){
				LifeCycleHolder.getPostProcessChainExecuteLifeCycleList().forEach(
						postProcessChainExecuteLifeCycle -> postProcessChainExecuteLifeCycle.postProcessAfterChainExecute(chainId, slot)
				);
			}
		}
		catch (ChainEndException e) {
			// 这里单独catch ChainEndException是因为ChainEndException是用户自己setIsEnd抛出的异常
			// 是属于正常逻辑，所以会在FlowExecutor中判断。这里不作为异常处理
			throw e;
		}
		catch (Exception e) {
			// 这里事先取到exception set到slot里，为了方便finally取到exception
			if (slot.isSubChain(chainId)) {
				slot.setSubException(chainId, e);
			}
			else {
				slot.setException(e);
			}
			throw e;
		}
	}

	public void executeRoute(Integer slotIndex) throws Exception {
		if (routeItem == null) {
			throw new FlowSystemException("no route condition or node in this chain[" + chainId + "]");
		}
		Slot slot = DataBus.getSlot(slotIndex);
		try {
			// 设置主ChainName
			slot.setChainId(chainId);

			// 执行决策路由
			routeItem.setCurrChainId(chainId);
			routeItem.execute(slotIndex);

			boolean routeResult = routeItem.getItemResultMetaValue(slotIndex);

			slot.setRouteResult(routeResult);
		}
		catch (ChainEndException e) {
			throw e;
		}
		catch (Exception e) {
			slot.setException(e);
			throw e;
		}
	}

	@Override
	public ExecuteableTypeEnum getExecuteType() {
		return ExecuteableTypeEnum.CHAIN;
	}

	@Override
	public void setId(String id) {
		this.chainId = id;
	}

	@Override
	public String getId() {
		return chainId;
	}

	@Override
	public void setTag(String tag) {
		//do nothing
	}

	@Override
	public String getTag() {
		return null;
	}

	public Executable getRouteItem() {
		return routeItem;
	}

	public void setRouteItem(Executable routeItem) {
		this.routeItem = routeItem;
	}

	public String getEl() {
		return el;
	}

	public void setEl(String el) {
		this.el = el;
	}

	public boolean isCompiled() {
		return isCompiled;
	}

	public void setCompiled(boolean compiled) {
		isCompiled = compiled;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

    public String getThreadPoolExecutorClass() {
        return threadPoolExecutorClass;
    }

    public void setThreadPoolExecutorClass(String threadPoolExecutorClass) {
        this.threadPoolExecutorClass = threadPoolExecutorClass;
    }

	// 构建临时的ConditionList
	private List<Condition> buildTemporaryConditionList() {
		// 构建临时chain
		String tempChainId = chainId +  "_temp_" + IdUtil.simpleUUID();
		Chain tempChain = new Chain(tempChainId);
		tempChain.setEl(el);
		tempChain.setCompiled(false);
		LiteFlowChainELBuilder.buildUnCompileChain(tempChain);
		// 移除临时chain
		FlowBus.removeChain(tempChainId);

		List<Condition> tempConditionList = tempChain.getConditionList();
		if (CollUtil.isEmpty(tempConditionList)) {
			throw new FlowSystemException("no conditionList in this chain[" + chainId + "]");
		}

		// 打印警告，可用于排查临时chain与已有chain重名（几乎不可能发生）而将已有chain覆盖的情况
		LOG.warn("The conditionList of chain[{}] is empty, temporarily using chain[{}] (now removed) to build it.", chainId, tempChainId);
		return tempConditionList;
	}
}
