package com.yomahub.liteflow.lifecycle;

import com.yomahub.liteflow.slot.Slot;

/**
 * 生命周期接口
 * 执行FLowExecutor的时候
 *
 * @author Bryan.Zhang
 * @since 2.12.4
 */
public interface PostProcessFlowExecuteLifeCycle extends LifeCycle{

    void postProcessBeforeFlowExecute(String chainId, Slot slot);

    void postProcessAfterFlowExecute(String chainId, Slot slot);
}
