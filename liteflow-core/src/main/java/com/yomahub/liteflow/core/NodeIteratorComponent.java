package com.yomahub.liteflow.core;

import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;

import java.util.Iterator;

/**
 * ITERATOR迭代器循环组件抽象类
 *
 * @author Bryan.Zhang
 * @since 2.9.7
 */
public abstract class NodeIteratorComponent extends NodeComponent {

	@Override
	public void process() throws Exception {
		Iterator<?> it = processIterator();
		Slot slot = this.getSlot();
		slot.setIteratorResult(this.getMetaValueKey(), it);
	}

	public abstract Iterator<?> processIterator() throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<?> getItemResultMetaValue(Integer slotIndex) {
		return DataBus.getSlot(slotIndex).getIteratorResult(this.getMetaValueKey());
	}

}
