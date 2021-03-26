package com.yomahub.liteflow.springboot;

import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.util.ExecutorHelper;
import com.yomahub.liteflow.util.Shutdown;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;

/**
 * 线程池装配类
 * 这个装配前置条件是需要LiteflowConfig，LiteflowPropertyAutoConfiguration以及SpringAware
 * @author justin.xu
 */
@Configuration
@ConditionalOnBean(LiteflowConfig.class)
@AutoConfigureAfter({LiteflowPropertyAutoConfiguration.class})
public class LiteflowExecutorAutoConfiguration {

    @Bean("whenExecutors")
    public ExecutorService executorService(LiteflowConfig liteflowConfig) {
        int useWorker = liteflowConfig.getWhenMaxWorkers();
        int useQueue = liteflowConfig.getWhenQueueLimit();
        if (useWorker == 0) {
            useWorker = Runtime.getRuntime().availableProcessors() + 1;
        }

        if (useQueue < 512) {
            useQueue = 512;
        }

        return ExecutorHelper.buildExecutor(useWorker, useQueue, "liteflow-when-calls", false);
    }

    @Bean
    public Shutdown shutdown() {
        return new Shutdown();
    }
}
