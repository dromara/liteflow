package com.yomahub.liteflow.core;

import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;

/**
 * WHILE条件节点抽象类
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public abstract class NodeWhileComponent extends NodeComponent {

	@Override
	public void process() throws Exception {
		boolean whileFlag = processWhile();
		Slot slot = this.getSlot();
		slot.setWhileResult(this.getMetaValueKey(), whileFlag);
	}

	public abstract boolean processWhile() throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public Boolean getItemResultMetaValue(Integer slotIndex) {
		return DataBus.getSlot(slotIndex).getWhileResult(this.getMetaValueKey());
	}

}
