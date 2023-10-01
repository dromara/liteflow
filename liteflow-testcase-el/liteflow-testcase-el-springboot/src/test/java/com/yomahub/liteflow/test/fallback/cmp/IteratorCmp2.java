package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeIteratorComponent;

import java.util.Collections;
import java.util.Iterator;

@LiteflowComponent("itn2")
@FallbackCmp
public class IteratorCmp2 extends NodeIteratorComponent {
    
    @Override
    public Iterator<?> processIterator() throws Exception {
        return Collections.emptyIterator();
    }
}
