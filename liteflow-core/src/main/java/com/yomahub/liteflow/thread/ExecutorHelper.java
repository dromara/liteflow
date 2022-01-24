/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.thread;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Maps;
import com.yomahub.liteflow.exception.ThreadExecutorServiceCreateException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.util.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;


/**
 * 线程池工具类
 *
 * @author Bryan.Zhang
 */
public class ExecutorHelper {

    private final Logger LOG = LoggerFactory.getLogger(ExecutorHelper.class);

    private static ExecutorHelper executorHelper;

    private ExecutorService executorService;

    /**
     * 此处使用Map缓存线程池信息
     * key - 线程池构建者的Class全类名
     * value - 线程池对象
     */
    private final Map<String, ExecutorService> executorServiceMap;

    private ExecutorHelper() {
        executorServiceMap = Maps.newConcurrentMap();
    }

    public static ExecutorHelper loadInstance() {
        if (ObjectUtil.isNull(executorHelper)) {
            executorHelper = new ExecutorHelper();
        }
        return executorHelper;
    }

    /**
     * 使用默认的等待时间1分钟，来关闭目标线程组。
     * <p>
     *
     * @param pool 需要关闭的线程组.
     */
    public void shutdownAwaitTermination(ExecutorService pool) {
        shutdownAwaitTermination(pool, 60L);
    }

    /**
     * 关闭ExecutorService的线程管理者
     * <p>
     *
     * @param pool    需要关闭的管理者
     * @param timeout 等待时间
     */
    public void shutdownAwaitTermination(ExecutorService pool,
                                         long timeout) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
                    LOG.error("Pool did not terminate.");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 构建全局默认线程池
     */
    public ExecutorService buildExecutor() {
        if (ObjectUtil.isNull(executorService)) {
            LiteflowConfig liteflowConfig = SpringAware.getBean(LiteflowConfig.class);
            assert liteflowConfig != null;
            executorService = getExecutorBuilder(liteflowConfig.getThreadExecutorClass()).buildExecutor();
        }
        return executorService;
    }

    /**
     * <p>
     * 构建线程池执行器 - 支持多个when公用一个线程池
     * </p>
     *
     * @param threadExecutorClass : 线程池构建者的Class全类名
     * @return java.util.concurrent.ExecutorService
     * @author sikadai
     * @date 2022/1/21 23:00
     */
    public ExecutorService buildExecutor(String threadExecutorClass) {
        if (StrUtil.isBlank(threadExecutorClass)) {
            return buildExecutor();
        }
        ExecutorService executorServiceFromCache = executorServiceMap.get(threadExecutorClass);
        if (executorServiceFromCache != null) {
            return executorServiceFromCache;
        } else {
            ExecutorService executorService = getExecutorBuilder(threadExecutorClass).buildExecutor();
            executorServiceMap.put(threadExecutorClass, executorService);
            return executorService;
        }
    }

    /**
     * <p>
     * 根据线程执行构建者Class类名获取ExecutorBuilder实例
     * </p>
     *
     * @param threadExecutorClass
     * @return com.yomahub.liteflow.thread.ExecutorBuilder
     * @author sikadai
     * @date 2022/1/21 23:04
     */
    private ExecutorBuilder getExecutorBuilder(String threadExecutorClass) {
        try {
            return (ExecutorBuilder) Class.forName(threadExecutorClass).newInstance();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ThreadExecutorServiceCreateException(e.getMessage());
        }

    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
