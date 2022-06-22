/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.core;

/**
 * 条件路由节点抽象类
 * @author Bryan.Zhang
 */
public abstract class NodeSwitchComponent extends NodeComponent {

	@Override
	public void process() throws Exception {
		String nodeId = this.processCond();
		this.getSlot().setCondResult(this.getClass().getName(), nodeId);
	}

	//用以返回路由节点的beanId
	public abstract String processCond() throws Exception;

}
