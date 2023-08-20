package com.yomahub.liteflow.spi.solon;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.spi.CmpAroundAspect;
import org.noear.solon.Solon;

/**
 * Solon 环境全局组件切面实现
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class SolonCmpAroundAspect implements CmpAroundAspect {

	public static ICmpAroundAspect cmpAroundAspect;

	static {
		Solon.context().getBeanAsync(ICmpAroundAspect.class, bean -> {
			cmpAroundAspect = bean;
		});
	}

	@Override
	public void beforeProcess(NodeComponent cmp) {
		if (ObjectUtil.isNotNull(cmpAroundAspect)) {
			cmpAroundAspect.beforeProcess(cmp);
		}
	}

	@Override
	public void afterProcess(NodeComponent cmp) {
		if (ObjectUtil.isNotNull(cmpAroundAspect)) {
			cmpAroundAspect.afterProcess(cmp);
		}
	}

	@Override
	public void onSuccess(NodeComponent cmp) {
		if (ObjectUtil.isNotNull(cmpAroundAspect)) {
			cmpAroundAspect.onSuccess(cmp);
		}
	}

	@Override
	public void onError(NodeComponent cmp, Exception e) {
		if (ObjectUtil.isNotNull(cmpAroundAspect)) {
			cmpAroundAspect.onError(cmp, e);
		}
	}

	@Override
	public int priority() {
		return 1;
	}

}
