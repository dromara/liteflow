package com.yomahub.liteflow.test.maxWaitSeconds.cmp;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

import java.util.Iterator;
import java.util.List;

@LiteflowComponent("x")
@LiteflowCmpDefine(NodeTypeEnum.ITERATOR)
public class XCmp {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_ITERATOR, nodeType = NodeTypeEnum.ITERATOR)
    public Iterator<?> processIterator(NodeComponent bindCmp) throws Exception {
        List<String> list = ListUtil.toList("one", "two");
        return list.iterator();
    }
}
