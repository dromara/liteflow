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

	//condition 类型 参数:ConditionTypeEnum 包含:then when
	private String conditionType;

	private List<Executable> nodeList;

	//只在when类型下有效，以区分当when调用链调用失败时是否继续往下执行 默认true继续执行
	private boolean errorResume = true;

	//只在when类型下有效，用于不同node进行同组合并，相同的组会进行合并，不同的组不会进行合并
	private String group = LocalDefaultFlowConstant.DEFAULT;

	//只在when类型下有效，为true的话说明在多个并行节点下，任意一个成功，整个when就成功
	private boolean any = false;

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

	public boolean isAny() {
		return any;
	}

	public void setAny(boolean any) {
		this.any = any;
	}
}
