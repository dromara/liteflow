package com.yomahub.liteflow.core;

import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;

import java.util.Iterator;

/**
 * ITERATOR迭代器循环组件抽象类
 * @author Bryan.Zhang
 * @since 2.9.7
 */
public abstract class NodeIteratorComponent extends NodeComponent{

    @Override
    public void process() throws Exception {
        Iterator<?> it = processIterator();
        Slot slot = this.getSlot();
        Class<?> originalClass = LiteFlowProxyUtil.getUserClass(this.getClass());
        slot.setIteratorResult(originalClass.getName(), it);
    }

    public abstract Iterator<?> processIterator() throws Exception;
}
