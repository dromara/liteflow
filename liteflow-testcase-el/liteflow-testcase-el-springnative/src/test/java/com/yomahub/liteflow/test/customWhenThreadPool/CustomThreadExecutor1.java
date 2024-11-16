package com.yomahub.liteflow.test.customWhenThreadPool;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.thread.ExecutorBuilder;

import java.util.concurrent.ExecutorService;

public class CustomThreadExecutor1 implements ExecutorBuilder {

	@Override
	public ExecutorService buildExecutor() {
		LiteflowConfig liteflowConfig = ContextAwareHolder.loadContextAware().getBean(LiteflowConfig.class);
		// 只有在非spring的场景下liteflowConfig才会为null
		if (ObjectUtil.isNull(liteflowConfig)) {
			liteflowConfig = new LiteflowConfig();
		}
        return buildDefaultExecutor(liteflowConfig.getGlobalThreadPoolSize(), liteflowConfig.getGlobalThreadPoolSize(),
                                    liteflowConfig.getGlobalThreadPoolQueueSize(), "customer-when-1-thead-");
	}

}
