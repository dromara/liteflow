package com.yomahub.liteflow.springboot;

import com.yomahub.liteflow.util.ExecutorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;

/**
 * desc :
 * name : Shutdown
 *
 * @author : xujia
 * date : 2021/3/24
 * @since : 1.8
 */
@Order(Integer.MIN_VALUE)
@Component
public class Shutdown {

    private static final Logger LOG = LoggerFactory.getLogger(Shutdown.class);

    @Resource(name = "parallelExecutor")
    private ExecutorService parallelExecutor;

    @PreDestroy
    public void destroy() throws Exception {
        LOG.info("Start closing the parallel-executors...");
        ExecutorHelper.shutdownAwaitTermination(parallelExecutor, 3600);
        LOG.info("Succeed closing the parallel-executors ok...");
    }

}
