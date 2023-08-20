package com.yomahub.liteflow.test.maxWaitSeconds.cmp;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeIteratorComponent;

import java.util.Iterator;
import java.util.List;

@LiteflowComponent("x")
public class XCmp extends NodeIteratorComponent {
    @Override
    public Iterator<?> processIterator() throws Exception {
        List<String> list = ListUtil.toList("one", "two");
        return list.iterator();
    }
}
