package com.yomahub.liteflow.test.maxWaitMilliseconds.cmp;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.NodeIteratorComponent;

import java.util.Iterator;
import java.util.List;

public class XCmp extends NodeIteratorComponent {
    @Override
    public Iterator<?> processIterator() throws Exception {
        List<String> list = ListUtil.toList("one", "two");
        return list.iterator();
    }
}
