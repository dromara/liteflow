package com.yomahub.liteflow.test.iterator.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeIteratorComponent;
import com.yomahub.liteflow.slot.DefaultContext;

import java.util.Iterator;
import java.util.List;

@LiteflowComponent("x3")
public class X3Cmp extends NodeIteratorComponent {
    @Override
    public Iterator<?> processIterator() throws Exception {
        DefaultContext context = this.getFirstContextBean();
        List<String> list = context.getData("list3");
        return list.iterator();
    }
}
