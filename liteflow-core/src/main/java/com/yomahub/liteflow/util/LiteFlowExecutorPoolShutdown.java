package com.yomahub.liteflow.util;

import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.thread.ExecutorHelper;

import java.util.concurrent.ExecutorService;

/**
 * 关闭shutdown类 执行清理工作
 *
 * @author Bryan.Zhang
 */
public class LiteFlowExecutorPoolShutdown {

	private static final LFLog LOG = LFLoggerManager.getLogger(LiteFlowExecutorPoolShutdown.class);

	public void destroy() throws Exception {
		ExecutorService executorService = ContextAwareHolder.loadContextAware().getBean("whenExecutors");

		LOG.info("Start closing the liteflow-when-calls...");
		ExecutorHelper.loadInstance().shutdownAwaitTermination(executorService);
		LOG.info("Succeed closing the liteflow-when-calls ok...");
	}

}
