package com.yomahub.liteflow.test.rollback.cmp;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.NodeIteratorComponent;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component("i")
public class ICmp extends NodeIteratorComponent {

    @Override
    public Iterator<?> processIterator() throws Exception {
        List<String> list = ListUtil.toList("jack", "mary", "tom");
        return list.iterator();
    }

    @Override
    public void rollback() throws Exception {
        System.out.println("ICmp rollback!");
    }
}
