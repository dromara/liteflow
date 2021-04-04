/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.flow;

import com.yomahub.liteflow.common.LocalDefaultFlowConstant;

import java.util.List;

/**
 * 里面包含了when或者then
 * @author Bryan.Zhang
 */
public class Condition {
	//	增加errorResume属性，以区分当when调用链调用失败时是否继续往下执行 默认true继续执行
	private boolean errorResume = true;
	// 增加groupId属性，用于不同node进行同组合并
	private String group = LocalDefaultFlowConstant.DEFAULT;
	// condition 类型 参数:ConditionTypeEnum 包含:then when
	private String conditionType;

	private List<Executable> nodeList;

	public Condition(List<Executable> nodeList) {
		this.nodeList = nodeList;
	}
	public Condition() {
	}

	public List<Executable> getNodeList() {
		return nodeList;
	}

	public void setNodeList(List<Executable> nodeList) {
		this.nodeList = nodeList;
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

	public String getConditionType() {
		return conditionType;
	}

	public void setConditionType(String conditionType) {
		this.conditionType = conditionType;
	}
}
