package com.yomahub.liteflow.test.iterator.cmp;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeIteratorComponent;
import com.yomahub.liteflow.slot.DefaultContext;

import java.util.Iterator;
import java.util.List;

@LiteflowComponent("x2")
public class X2Cmp extends NodeIteratorComponent {
    @Override
    public Iterator<?> processIterator() throws Exception {
        DefaultContext context = this.getFirstContextBean();
        List<String> list = context.getData("list2");
        return list.iterator();
    }
}
