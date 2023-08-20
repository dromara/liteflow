package com.yomahub.liteflow.spi.local;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.spi.CmpAroundAspect;

/**
 * 非Spring环境全局组件切面实现 其实非Spring不支持全局组件切面，所以这个是个空实现
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class LocalCmpAroundAspect implements CmpAroundAspect {

	@Override
	public void beforeProcess(NodeComponent cmp) {
		// 无spring环境下为空实现
	}

	@Override
	public void afterProcess(NodeComponent cmp) {
		// 无spring环境下为空实现
	}

	@Override
	public void onSuccess(NodeComponent cmp) {
		// 无spring环境下为空实现
	}

	@Override
	public void onError(NodeComponent cmp, Exception e) {
		// 无spring环境下为空实现
	}

	@Override
	public int priority() {
		return 2;
	}

}
