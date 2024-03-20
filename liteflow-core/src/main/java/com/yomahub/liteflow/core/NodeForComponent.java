package com.yomahub.liteflow.core;

import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;

/**
 * FOR计数节点抽象类
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public abstract class NodeForComponent extends NodeComponent {

	@Override
	public void process() throws Exception {
		int forCount = processFor();
		Slot slot = this.getSlot();
		slot.setForResult(this.getMetaValueKey(), forCount);
	}

	public abstract int processFor() throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public Integer getItemResultMetaValue(Integer slotIndex) {
		return DataBus.getSlot(slotIndex).getForResult(this.getMetaValueKey());
	}

}
