package com.yomahub.liteflow.core;

import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;

/**
 * 循环跳出节点逻辑抽象类
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public abstract class NodeBreakComponent extends NodeComponent {

	@Override
	public void process() throws Exception {
		boolean breakFlag = processBreak();
		Slot slot = this.getSlot();
		slot.setBreakResult(this.getMetaValueKey(), breakFlag);
	}

	public abstract boolean processBreak() throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public Boolean getItemResultMetaValue(Integer slotIndex) {
		return DataBus.getSlot(slotIndex).getBreakResult(this.getMetaValueKey());
	}

}
