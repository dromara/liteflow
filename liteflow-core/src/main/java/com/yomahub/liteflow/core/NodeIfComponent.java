package com.yomahub.liteflow.core;

import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;

/**
 * IF节点抽象类
 *
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public abstract class NodeIfComponent extends NodeComponent {

	@Override
	public void process() throws Exception {
		boolean result = this.processIf();
		this.getSlot().setIfResult(this.getMetaValueKey(), result);
	}

	public abstract boolean processIf() throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public Boolean getItemResultMetaValue(Integer slotIndex) {
		return DataBus.getSlot(slotIndex).getIfResult(this.getMetaValueKey());
	}
}
