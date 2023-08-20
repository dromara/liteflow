package com.yomahub.liteflow.spi;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.Slot;

/**
 * 组件全局切面spi接口
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public interface CmpAroundAspect extends SpiPriority {

	void beforeProcess(NodeComponent cmp);

	void afterProcess(NodeComponent cmp);

	void onSuccess(NodeComponent cmp);

	void onError(NodeComponent cmp, Exception e);

}
