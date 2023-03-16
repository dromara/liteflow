/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.enums.ExecuteTypeEnum;
import com.yomahub.liteflow.exception.ChainEndException;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Condition的抽象类
 * @author Bryan.Zhang
 */
public abstract class Condition implements Executable{

	private String id;

	/**
	 * 可执行元素的集合
	 */
	private final Map<String, List<Executable>> executableGroup = new HashMap<>();

	/**
	 * 当前所在的ChainName
	 * 如果对于子流程来说，那这个就是子流程所在的Chain
	 */
	private String currChainId;

	@Override
	public void execute(Integer slotIndex) throws Exception {
		try{
			executeCondition(slotIndex);
		}catch (ChainEndException e){
			//这里单独catch ChainEndException是因为ChainEndException是用户自己setIsEnd抛出的异常
			//是属于正常逻辑，所以会在FlowExecutor中判断。这里不作为异常处理
			throw e;
		}catch (Exception e){
			Slot slot = DataBus.getSlot(slotIndex);
			String chainId = this.getCurrChainId();
			//这里事先取到exception set到slot里，为了方便finally取到exception
			if (slot.isSubChain(chainId)){
				slot.setSubException(chainId, e);
			}else{
				slot.setException(e);
			}
			throw e;
		}
	}

	protected abstract void executeCondition(Integer slotIndex) throws Exception;

	@Override
	public ExecuteTypeEnum getExecuteType() {
		return ExecuteTypeEnum.CONDITION;
	}

	@Override
	public String getExecuteId() {
		return this.id;
	}

	public List<Executable> getExecutableList() {
		return getExecutableList(ConditionKey.DEFAULT_KEY);
	}

	public List<Executable> getExecutableList(String groupKey) {
		List<Executable> executableList = this.executableGroup.get(groupKey);
		if (CollUtil.isEmpty(executableList)){
			executableList = new ArrayList<>();
		}
		return executableList;
	}

	public Executable getExecutableOne(String groupKey) {
		List<Executable> list = getExecutableList(groupKey);
		if (CollUtil.isEmpty(list)){
			return null;
		}else{
			return list.get(0);
		}
	}

	public void setExecutableList(List<Executable> executableList) {
		this.executableGroup.put(ConditionKey.DEFAULT_KEY, executableList);
	}

	public void addExecutable(Executable executable) {
		addExecutable(ConditionKey.DEFAULT_KEY, executable);
	}

	public void addExecutable(String groupKey, Executable executable) {
		if (ObjectUtil.isNull(executable)){
			return;
		}
		List<Executable> executableList = this.executableGroup.get(groupKey);
		if (CollUtil.isEmpty(executableList)){
			this.executableGroup.put(groupKey, ListUtil.toList(executable));
		}else{
			this.executableGroup.get(groupKey).add(executable);
		}
	}

	public abstract ConditionTypeEnum getConditionType();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * 
	 * @deprecated 请使用 {@link #setCurrChainId(String)}
	 */
	@Deprecated
	public String getCurrChainName() {
		return currChainId;
	}

	public String getCurrChainId() {
		return currChainId;
	}
	
	@Override
	public void setCurrChainId(String currChainId) {
		this.currChainId = currChainId;
	}

	public Map<String, List<Executable>> getExecutableGroup() {
		return executableGroup;
	}
}
