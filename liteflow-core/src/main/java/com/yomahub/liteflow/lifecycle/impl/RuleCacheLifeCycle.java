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

import java.util.concurrent.ConcurrentMap;

/**
 * Chain执行前的缓存处理
 * @author DaleLee
 * @since 2.13.0
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
        // 记录chainId在缓存中
        // 这里不记录实际的chain是因为chainId对应的chain之后有可能在FlowBus中被移除
        // 或被更新替换，以FlowBus中实际存在的chain为准
        cache.get(chainId, key -> PRESENT);
    }

    @Override
    public void postProcessAfterFlowExecute(String chainId, Slot slot) {
        // chain执行时，有可能在未编译前就被淘汰
        // 结果使被淘汰的chain仍持有condition（淘汰后就立刻编译）
        // 这里做兜底操作，执行完后再次判断其是否在缓存中
        // 若不在则清空chain的condition
        ConcurrentMap<@NonNull String, @NonNull Object> concurrentMap = cache.asMap();
        concurrentMap.computeIfAbsent(chainId, key -> {
            cleanChain(chainId);
            return null;
        });

    }

    public Cache<String, Object> getCache() {
        return cache;
    }

    /**
     * 监听在缓存中被移除的chain
     */
    private static class ChainRemovalListener implements RemovalListener<String, Object> {

        @Override
        public void onRemoval(@Nullable String chainId, @Nullable Object object, @NonNull RemovalCause removalCause) {
            cleanChain(chainId);
        }
    }

    private static void cleanChain(String chainId) {
        Chain chain = FlowBus.getChain(chainId);
        // chain可能已经在FlowBus中被移除了
        if (ObjectUtil.isNull(chain)) {
            return;
        }
        // 清空condition并将chain设置为未编译
        chain.setConditionList(null);
        chain.setCompiled(false);
    }
}
