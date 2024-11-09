package com.yomahub.liteflow.flow.cache;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.lifecycle.PostProcessFlowExecuteLifeCycle;
import com.yomahub.liteflow.slot.Slot;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * Chain执行前的缓存处理
 * @author DaleLee
 * @since
 */
public class RuleCachePostProcessFlowExecuteLifeCycle implements PostProcessFlowExecuteLifeCycle {
    /**
     * 缓存
     */
    private final Cache<String, Chain> cache;

    public RuleCachePostProcessFlowExecuteLifeCycle(int capacity) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(capacity)
                .evictionListener(new ChainRemovalListener())
                .build();
    }

    @Override
    public void postProcessBeforeFlowExecute(String chainId, Slot slot) {
        Chain chain = FlowBus.getChain(chainId);
        if (ObjectUtil.isNull(chain)) {
            return;
        }
        // 记录在缓存中
        cache.put(chainId, chain);
    }

    @Override
    public void postProcessAfterFlowExecute(String chainId, Slot slot) {

    }

    /**
     * 监听在缓存中被移除的chain
     */
    private static class ChainRemovalListener implements RemovalListener<String, Chain> {

        @Override
        public void onRemoval(@Nullable String chanId, @Nullable Chain chain, @NonNull RemovalCause removalCause) {
            List<Condition> conditionList = chain.getConditionList();
            // 清空condition 并将chain设置为未编译
            if (CollUtil.isNotEmpty(conditionList)) {
                conditionList.clear();
            }
            chain.setCompiled(false);
        }
    }
}
