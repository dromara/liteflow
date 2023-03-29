package com.yomahub.liteflow.core;

import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;

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
		Class<?> originalClass = LiteFlowProxyUtil.getUserClass(this.getClass());
		slot.setWhileResult(originalClass.getName(), whileFlag);
	}

	public abstract boolean processWhile() throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public Boolean getItemResultMetaValue(Integer slotIndex) {
		Class<?> originalClass = LiteFlowProxyUtil.getUserClass(this.getClass());
		return DataBus.getSlot(slotIndex).getWhileResult(originalClass.getName());
	}

}
