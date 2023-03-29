package com.yomahub.liteflow.core;

import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;

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
		Class<?> originalClass = LiteFlowProxyUtil.getUserClass(this.getClass());
		slot.setForResult(originalClass.getName(), forCount);
	}

	public abstract int processFor() throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public Integer getItemResultMetaValue(Integer slotIndex) {
		Class<?> originalClass = LiteFlowProxyUtil.getUserClass(this.getClass());
		return DataBus.getSlot(slotIndex).getForResult(originalClass.getName());
	}

}
