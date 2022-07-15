/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow.element.condition;

import com.yomahub.liteflow.common.LocalDefaultFlowConstant;
import com.yomahub.liteflow.enums.ExecuteTypeEnum;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.enums.ConditionTypeEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Condition的抽象类
 * @author Bryan.Zhang
 */
public abstract class Condition implements Executable{

	private String id;

	//可执行元素的集合
	private List<Executable> executableList = new ArrayList<>();

	//只在when类型下有效，以区分当when调用链调用失败时是否继续往下执行 默认false不继续执行
	private boolean errorResume = false;

	//只在when类型下有效，用于不同node进行同组合并，相同的组会进行合并，不同的组不会进行合并
	private String group = LocalDefaultFlowConstant.DEFAULT;

	//只在when类型下有效，为true的话说明在多个并行节点下，任意一个成功，整个when就成功
	private boolean any = false;

	// when单独的线程池名称
	private String threadExecutorClass;

	//当前所在的ChainName
	//如果对于子流程来说，那这个就是子流程所在的Chain
	private String currChainName;

	@Override
	public ExecuteTypeEnum getExecuteType() {
		return ExecuteTypeEnum.CONDITION;
	}

	@Override
	public String getExecuteName() {
		return this.id;
	}

	public List<Executable> getExecutableList() {
		return executableList;
	}

	public void setExecutableList(List<Executable> executableList) {
		this.executableList = executableList;
	}

	public void addExecutable(Executable executable) {
		this.executableList.add(executable);
	}

	public boolean isErrorResume() {
		return errorResume;
	}

	public void setErrorResume(boolean errorResume) {
		this.errorResume = errorResume;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public abstract ConditionTypeEnum getConditionType();

	public boolean isAny() {
		return any;
	}

	public void setAny(boolean any) {
		this.any = any;
	}

	public String getThreadExecutorClass() {
		return threadExecutorClass;
	}

	public void setThreadExecutorClass(String threadExecutorClass) {
		this.threadExecutorClass = threadExecutorClass;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCurrChainName() {
		return currChainName;
	}

	@Override
	public void setCurrChainName(String currChainName) {
		this.currChainName = currChainName;
	}
}
