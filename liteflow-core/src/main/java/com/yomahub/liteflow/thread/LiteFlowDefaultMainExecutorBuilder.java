package com.yomahub.liteflow.thread;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;

import java.util.concurrent.ExecutorService;

/**
 * LiteFlow 默认主执行器生成器
 *
 * @author Yun
 */
public class LiteFlowDefaultMainExecutorBuilder implements ExecutorBuilder {

	@Override
	public ExecutorService buildExecutor() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		// 只有在非spring的场景下liteflowConfig才会为null
		if (ObjectUtil.isNull(liteflowConfig)) {
			liteflowConfig = new LiteflowConfig();
		}
		return buildDefaultExecutor(liteflowConfig.getMainExecutorWorks(), liteflowConfig.getMainExecutorWorks()*2, 200,
				"main-thread-");
	}

}
