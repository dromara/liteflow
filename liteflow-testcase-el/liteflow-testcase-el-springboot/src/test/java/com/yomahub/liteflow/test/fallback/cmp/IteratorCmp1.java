package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeIteratorComponent;

import java.util.Arrays;
import java.util.Iterator;

@LiteflowComponent("itn1")
public class IteratorCmp1 extends NodeIteratorComponent {

    @Override
    public Iterator<?> processIterator() throws Exception {
        return Arrays.asList("a", "b", "c").iterator();
    }
}
