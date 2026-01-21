package com.yomahub.liteflow.flow.element.condition;

import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;

/**
 * Chain bind 包装 Condition
 * 用于在对 Chain 进行 bind 操作时，创建一个包装 Condition 来持有 bind 数据，
 * 而不是直接修改 Chain 内部的 Node，从而避免多个 chain 引用同一个子 chain 时的 bind 数据污染问题。
 *
 * @author Bryan.Zhang
 * @since 2.15.3
 */
public class ChainBindWrapperCondition extends Condition {

    private final Chain wrappedChain;

    public ChainBindWrapperCondition(Chain chain) {
        this.wrappedChain = chain;
    }

    @Override
    public void executeCondition(Integer slotIndex) throws Exception {
        // 设置当前 chainId
        wrappedChain.setCurrChainId(this.getCurrChainId());
        // 执行被包装的 chain
        wrappedChain.execute(slotIndex);
    }

    @Override
    public ConditionTypeEnum getConditionType() {
        return ConditionTypeEnum.TYPE_CHAIN_BIND_WRAPPER;
    }

    public Chain getWrappedChain() {
        return wrappedChain;
    }

    @Override
    public String getId() {
        return "chain_bind_wrapper_" + wrappedChain.getChainId();
    }
}
