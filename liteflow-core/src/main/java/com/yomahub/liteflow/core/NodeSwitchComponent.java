/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.core;

import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;

/**
 * 条件路由节点抽象类
 *
 * @author Bryan.Zhang
 */
public abstract class NodeSwitchComponent extends NodeComponent {

	@Override
	public void process() throws Exception {
		String nodeId = this.processSwitch();
		this.getSlot().setSwitchResult(this.getMetaValueKey(), nodeId);
	}

	// 用以返回路由节点的beanId
	public abstract String processSwitch() throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public String getItemResultMetaValue(Integer slotIndex) {
		return DataBus.getSlot(slotIndex).getSwitchResult(this.getMetaValueKey());
	}

}
