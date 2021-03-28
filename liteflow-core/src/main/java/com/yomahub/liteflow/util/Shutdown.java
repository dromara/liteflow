package com.yomahub.liteflow.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;

/**
 * 关闭shutdown类
 * 执行清理工作
 * @author Bryan.Zhang
 */
public class Shutdown {

    private static final Logger LOG = LoggerFactory.getLogger(Shutdown.class);

    @PreDestroy
    public void destroy() throws Exception {
        ExecutorService executorService = SpringAware.getBean("whenExecutors");

        LOG.info("Start closing the liteflow-when-calls...");
        ExecutorHelper.shutdownAwaitTermination(executorService);
        LOG.info("Succeed closing the liteflow-when-calls ok...");
    }
}
