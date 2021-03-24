/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.flow;

import java.util.List;

/**
 * 里面包含了when或者then
 * @author Bryan.Zhang
 */
public class Condition {

	private List<Executable> nodeList;

	public Condition(List<Executable> nodeList) {
		this.nodeList = nodeList;
	}

	public List<Executable> getNodeList() {
		return nodeList;
	}

	public void setNodeList(List<Executable> nodeList) {
		this.nodeList = nodeList;
	}
}
