package com.yomahub.liteflow.spi.spring;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.spi.CmpAroundAspect;
import com.yomahub.liteflow.spring.ComponentScanner;

/**
 * Spring环境全局组件切面实现
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class SpringCmpAroundAspect implements CmpAroundAspect {

	@Override
	public void beforeProcess(NodeComponent cmp) {
		if (ObjectUtil.isNotNull(ComponentScanner.cmpAroundAspect)) {
			ComponentScanner.cmpAroundAspect.beforeProcess(cmp);
		}
	}

	@Override
	public void afterProcess(NodeComponent cmp) {
		if (ObjectUtil.isNotNull(ComponentScanner.cmpAroundAspect)) {
			ComponentScanner.cmpAroundAspect.afterProcess(cmp);
		}
	}

	@Override
	public void onSuccess(NodeComponent cmp) {
		if (ObjectUtil.isNotNull(ComponentScanner.cmpAroundAspect)) {
			ComponentScanner.cmpAroundAspect.onSuccess(cmp);
		}
	}

	@Override
	public void onError(NodeComponent cmp, Exception e) {
		if (ObjectUtil.isNotNull(ComponentScanner.cmpAroundAspect)) {
			ComponentScanner.cmpAroundAspect.onError(cmp, e);
		}
	}

	@Override
	public int priority() {
		return 1;
	}

}
