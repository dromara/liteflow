/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow.element.condition;

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



	//当前所在的ChainName
	//如果对于子流程来说，那这个就是子流程所在的Chain
	private String currChainName;

	@Override
	public ExecuteTypeEnum getExecuteType() {
		return ExecuteTypeEnum.CONDITION;
	}

	@Override
	public String getExecuteId() {
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

	public abstract ConditionTypeEnum getConditionType();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * 
	 * @return
	 * @deprecated 请使用 {@link #setCurrChainId(String)}
	 */
	@Deprecated
	public String getCurrChainName() {
		return currChainName;
	}

	public String getCurrChainId() {
		return currChainName;
	}
	
	@Override
	public void setCurrChainId(String currChainName) {
		this.currChainName = currChainName;
	}
}
