package com.yomahub.liteflow.test.customWhenThreadPool;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.thread.ExecutorBuilder;
import com.yomahub.liteflow.util.SpringAware;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class CustomThreadExecutor1 implements ExecutorBuilder {

    @Override
    public ExecutorService buildExecutor() {
        LiteflowConfig liteflowConfig = SpringAware.getBean(LiteflowConfig.class);
        //只有在非spring的场景下liteflowConfig才会为null
        if (ObjectUtil.isNull(liteflowConfig)) {
            liteflowConfig = new LiteflowConfig();
        }
        return TtlExecutors.getTtlExecutorService(new ThreadPoolExecutor(liteflowConfig.getWhenMaxWorkers(),
                liteflowConfig.getWhenMaxWorkers(),
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(liteflowConfig.getWhenQueueLimit()),
                new ThreadFactory() {
                    private final AtomicLong number = new AtomicLong();

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread newThread = Executors.defaultThreadFactory().newThread(r);
                        newThread.setName("Customer-when-1-thead-" + number.getAndIncrement());
                        newThread.setDaemon(false);
                        return newThread;
                    }
                },
                new ThreadPoolExecutor.AbortPolicy()));
    }
}
