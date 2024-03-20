package com.yomahub.liteflow.test.fallback.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;

import java.util.HashSet;
import java.util.Set;

public class WhileCmp1 extends NodeBooleanComponent {
    private int count = 0;

    // 执行过的 chain
    Set<String> executedChain = new HashSet<>();

    @Override
    public boolean processBoolean() throws Exception {
        // 判断是否切换了 chain
        if (!executedChain.contains(this.getCurrChainId())) {
            count = 0;
            executedChain.add(this.getCurrChainId());
        }
        count++;
        return count <= 3;
    }
}
