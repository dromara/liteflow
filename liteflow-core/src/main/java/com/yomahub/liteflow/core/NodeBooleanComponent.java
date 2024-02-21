package com.yomahub.liteflow.core;

import com.yomahub.liteflow.slot.DataBus;

/**
 * BOOLEAN类型的抽象节点
 *
 * @author Bryan.Zhang
 * @since 2.12.0
 */
public abstract class NodeBooleanComponent extends NodeComponent {

	@Override
	public void process() throws Exception {
		boolean result = this.processBoolean();
		this.getSlot().setIfResult(this.getMetaValueKey(), result);
	}

	public abstract boolean processBoolean() throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public Boolean getItemResultMetaValue(Integer slotIndex) {
		return DataBus.getSlot(slotIndex).getIfResult(this.getMetaValueKey());
	}
}
