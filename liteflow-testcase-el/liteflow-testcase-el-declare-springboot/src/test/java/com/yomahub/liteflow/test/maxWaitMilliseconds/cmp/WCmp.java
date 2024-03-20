package com.yomahub.liteflow.test.maxWaitMilliseconds.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

import java.util.HashSet;
import java.util.Set;

@LiteflowComponent("w")
@LiteflowCmpDefine(NodeTypeEnum.BOOLEAN)
public class WCmp {
    private int count = 0;

    // 执行过的 chain
    Set<String> executedChain = new HashSet<>();

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_BOOLEAN, nodeType = NodeTypeEnum.BOOLEAN)
    public boolean processWhile(NodeComponent bindCmp) throws Exception {
        // 判断是否切换了 chain
        if (!executedChain.contains(bindCmp.getCurrChainId())) {
            count = 0;
            executedChain.add(bindCmp.getCurrChainId());
        }
        count++;
        return count <= 2;
    }
}
