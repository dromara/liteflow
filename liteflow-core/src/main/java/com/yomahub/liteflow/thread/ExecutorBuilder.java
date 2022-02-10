package com.yomahub.liteflow.thread;

import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 并行多线程执行器构造器接口
 *
 * @author Bryan.Zhang
 * @since 2.6.6
 */
public interface ExecutorBuilder {

    ExecutorService buildExecutor();

    /**
     * <p>
     * 构建默认的线程池对象
     * </p>
     * @author sikadai
     * @date 2022/1/21 23:07
     * @param corePoolSize : 核心线程池数量
     * @param maximumPoolSize : 最大线程池数量
     * @param queueCapacity : 队列的容量
     * @param threadName  : 线程吃名称
     * @return java.util.concurrent.ExecutorService
     */
    default ExecutorService buildDefaultExecutor(int corePoolSize, int maximumPoolSize, int queueCapacity, String threadName) {
        return TtlExecutors.getTtlExecutorService(new ThreadPoolExecutor(corePoolSize,
                maximumPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadFactory() {
                    private final AtomicLong number = new AtomicLong();

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread newThread = Executors.defaultThreadFactory().newThread(r);
                        newThread.setName(threadName + number.getAndIncrement());
                        newThread.setDaemon(false);
                        return newThread;
                    }
                },
                new ThreadPoolExecutor.AbortPolicy()));
    }
}
