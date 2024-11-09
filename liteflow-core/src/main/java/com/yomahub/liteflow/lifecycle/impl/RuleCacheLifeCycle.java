package com.yomahub.liteflow.lifecycle.impl;

import cn.hutool.core.util.ObjectUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.lifecycle.PostProcessFlowExecuteLifeCycle;
import com.yomahub.liteflow.slot.Slot;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Chain执行前的缓存处理
 * @author DaleLee
 * @since
 */
public class RuleCacheLifeCycle implements PostProcessFlowExecuteLifeCycle {
    // 缓存
    private final Cache<String, Object> cache;
    // 在缓存中与key关联的虚拟值
    private static final Object PRESENT = new Object();

    public RuleCacheLifeCycle(int capacity) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(capacity)
                .evictionListener(new ChainRemovalListener())
                .build();
    }

    @Override
    public void postProcessBeforeFlowExecute(String chainId, Slot slot) {
        if (!FlowBus.containChain(chainId)) {
            return;
        }
        // 记录chainId在缓存中
        // 这里不记录实际的chain是因为chain之后有可能在FlowBus中被移除
        // 以FlowBus中实际存在的chain为准
        cache.put(chainId, PRESENT);
    }

    @Override
    public void postProcessAfterFlowExecute(String chainId, Slot slot) {

    }

    /**
     * 监听在缓存中被移除的chain
     */
    private static class ChainRemovalListener implements RemovalListener<String, Object> {

        @Override
        public void onRemoval(@Nullable String chanId, @Nullable Object object, @NonNull RemovalCause removalCause) {
            Chain chain = FlowBus.getChain(chanId);
            // chain可能已经在FlowBus中被移除了
            if (ObjectUtil.isNull(chain)) {
                return;
            }
            // 清空condition并将chain设置为未编译
            chain.setConditionList(null);
            chain.setCompiled(false);
        }
    }
}
