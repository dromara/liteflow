package com.yomahub.liteflow.lifecycle;

import com.yomahub.liteflow.slot.Slot;

/**
 * 生命周期接口
 * 执行Chain的时候
 *
 * @author Bryan.Zhang
 * @since 2.12.4
 */
public interface PostProcessChainExecuteLifeCycle extends LifeCycle{

    void postProcessBeforeChainExecute(String chainId, Slot slot);

    void postProcessAfterChainExecute(String chainId, Slot slot);
}
