package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

import java.util.Arrays;
import java.util.Iterator;

@LiteflowComponent("itn1")
@LiteflowCmpDefine(NodeTypeEnum.ITERATOR)
public class IteratorCmp1 {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_ITERATOR)
    public Iterator<?> processIterator(NodeComponent bindCmp) throws Exception {
        return Arrays.asList("a", "b", "c").iterator();
    }
}
