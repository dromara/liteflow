package com.yomahub.liteflow.test.retry.cmp;


import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.NodeIteratorComponent;

import java.util.Iterator;
import java.util.List;

public class ICmp extends NodeIteratorComponent {
    int flag = 0;
    @Override
    public Iterator<?> processIterator() throws Exception {
        flag ++;
        if(flag < 4) throw new RuntimeException();
        else {
            List<String> list = ListUtil.toList("jack");
            return list.iterator();
        }
    }
}
