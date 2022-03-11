package com.yomahub.liteflow.spi;

import com.yomahub.liteflow.entity.data.Slot;

/**
 * 组件全局切面spi接口
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public interface CmpAroundAspect extends SpiPriority {

    void beforeProcess(String nodeId, Slot slot);

    void afterProcess(String nodeId, Slot slot);
}
