package com.yomahub.liteflow.springboot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;

import static com.yomahub.liteflow.util.ExecutorHelper.buildExecutor;

/**
 * desc :
 * name : LiteflowExecutorAutoConfiguration
 *
 * @author : xujia
 * date : 2021/3/24
 * @since : 1.8
 */
@Configuration
public class LiteflowExecutorAutoConfiguration {

    @Bean("parallelExecutor")
    public ExecutorService parallelExecutor(
            @Value("${threadPool.parallel.worker:0}") int worker,
            @Value("${threadPool.parallel.queue:512}") int queue) {
        int useWorker = worker;
        int useQueue = queue;
        if (useWorker == 0) {
            useWorker = Runtime.getRuntime().availableProcessors() + 1;
        }

        if (useQueue < 512) {
            useQueue = 512;
        }

        return buildExecutor(useWorker, useQueue, "parallel-executors", false);
    }
}
