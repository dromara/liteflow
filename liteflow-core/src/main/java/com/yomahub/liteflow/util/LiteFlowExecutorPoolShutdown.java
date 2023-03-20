package com.yomahub.liteflow.util;

import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.thread.ExecutorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;

/**
 * 关闭shutdown类 执行清理工作
 *
 * @author Bryan.Zhang
 */
public class LiteFlowExecutorPoolShutdown {

	private static final Logger LOG = LoggerFactory.getLogger(LiteFlowExecutorPoolShutdown.class);

	@PreDestroy
	public void destroy() throws Exception {
		ExecutorService executorService = ContextAwareHolder.loadContextAware().getBean("whenExecutors");

		LOG.info("Start closing the liteflow-when-calls...");
		ExecutorHelper.loadInstance().shutdownAwaitTermination(executorService);
		LOG.info("Succeed closing the liteflow-when-calls ok...");
	}

}
