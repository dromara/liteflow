package com.yomahub.liteflow.lifecycle.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.lifecycle.PostProcessChainExecuteLifeCycle;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;
import com.yomahub.liteflow.slot.Slot;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * Chain 缓存处理
 * @author DaleLee
 * @since 2.13.0
 */
public class RuleCacheLifeCycle implements PostProcessChainExecuteLifeCycle {
    /**
     * 缓存
     */
    private final Cache<String, ChainState> cache;

    public RuleCacheLifeCycle(int capacity) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(capacity)
                .evictionListener(new ChainRemovalListener())
                .build();
    }

    @Override
    public void postProcessBeforeChainExecute(String chainId, Slot slot) {
        // 记录 chainId 在缓存中
        // 初始状态为 ACTIVE
        cache.get(chainId, key -> new ChainState(State.ACTIVE));
    }

    @Override
    public void postProcessAfterChainExecute(String chainId, Slot slot) {
        // 不在缓存中、或出于非活跃状态，但未被清理
        if (!isActive(chainId) && !isCleaned(chainId)) {
            cleanChain(chainId);
        }
    }

    /**
     * Chain 状态枚举
     */
    public enum State {
        /**
         * 活跃状态
         */
        ACTIVE,
        /**
         * 非活跃状态 (处于淘汰流程中)
         */
        INACTIVE
    }

    /**
     * Chain 在缓存中状态
     */
    public static class ChainState {
        /**
         * Chain 状态
         */
        private State state;

        public ChainState(State state) {
            this.state = state;
        }

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
        }
    }

    /**
     * 监听在缓存中被移除的 Chain
     */
    private static class ChainRemovalListener implements RemovalListener<String, ChainState> {

        @Override
        public void onRemoval(@Nullable String chainId, @Nullable ChainState chainState, @NonNull RemovalCause removalCause) {
            if (ObjectUtil.isNotNull(chainState)) {
                chainState.setState(State.INACTIVE);
            }
            cleanChain(chainId);
        }
    }

    /**
     * 获取缓存
     * @return cache
     */
    public Cache<String, ChainState> getCache() {
        return cache;
    }

    /**
     * 判断 Chain 的 Condition 是否被清理
     * @param chainId chainId
     * @return 被清理返回 true，否则返回 false
     */
    private boolean isCleaned(String chainId) {
        Chain chain = LiteflowMetaOperator.getChain(chainId);
        if (ObjectUtil.isNull(chain)) {
            return true;
        }
        List<Condition> conditionList = chain.getConditionList();
        return CollUtil.isEmpty(conditionList);
    }

    /**
     * 判断 Chain 在缓存中是活跃状态
     * @param chainId chainId
     * @return 活跃状态返回 true，不在缓存中或处于非活状态返回 false
     */
    private boolean isActive(String chainId) {
        ChainState chainState = cache.getIfPresent(chainId);
        return ObjectUtil.isNotNull(chainState)
            && State.ACTIVE.equals(chainState.getState());
    }

    /**
     * 清理 Chain 的 Condition
     * @param chainId chainId
     */
    private static void cleanChain(String chainId) {
        Chain chain = LiteflowMetaOperator.getChain(chainId);
        // chain可能已经在FlowBus中被移除了
        if (ObjectUtil.isNull(chain)) {
            return;
        }
        // 将chain设置为未编译并清空condition
        chain.setCompiled(false);
        chain.setConditionList(null);
    }
}
