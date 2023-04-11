package com.yomahub.liteflow.test.script.groovy.loop.cmp;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.core.NodeIteratorComponent;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component("e")
public class ECmp extends NodeIteratorComponent {
    @Override
    public Iterator<?> processIterator() throws Exception {
        List<String> list = CollUtil.toList("jack","tom","frank");
        return list.iterator();
    }
}
