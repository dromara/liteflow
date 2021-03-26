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
 * 关闭shutdown类
 * 执行清理工作
 * @author justin.xu
 */
@Order(Integer.MIN_VALUE)
@Component
public class Shutdown {

    private static final Logger LOG = LoggerFactory.getLogger(Shutdown.class);

    @Resource
    private ExecutorService executorService;

    @PreDestroy
    public void destroy() throws Exception {
        LOG.info("Start closing the liteflow-when-calls...");
        ExecutorHelper.shutdownAwaitTermination(executorService);
        LOG.info("Succeed closing the liteflow-when-calls ok...");
    }

}
