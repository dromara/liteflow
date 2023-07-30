package com.yomahub.liteflow.thread;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;

import java.util.concurrent.*;

/**
 * LiteFlow默认的并行多线程执行器实现
 *
 * @author Bryan.Zhang
 * @since 2.6.6
 */
public class LiteFlowDefaultWhenExecutorBuilder implements ExecutorBuilder {

	@Override
	public ExecutorService buildExecutor() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		// 只有在非spring的场景下liteflowConfig才会为null
		if (ObjectUtil.isNull(liteflowConfig)) {
			liteflowConfig = new LiteflowConfig();
		}
		return buildDefaultExecutor(liteflowConfig.getWhenMaxWorkers(), liteflowConfig.getWhenMaxWorkers(),
				liteflowConfig.getWhenQueueLimit(), "when-thread-");
	}

}
