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

    private Map<String, ExecutorService> executorServiceMap;

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

    public ExecutorService buildExecutor() {
        if (ObjectUtil.isNull(executorService)) {
            LiteflowConfig liteflowConfig = SpringAware.getBean(LiteflowConfig.class);
            assert liteflowConfig != null;
            executorService = buildExecutor(liteflowConfig.getThreadExecutorClass());
        }
        return executorService;
    }

    public ExecutorService buildExecutor(String threadExecutorClass) {
        try {
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
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ThreadExecutorServiceCreateException(e.getMessage());
        }
    }

    private ExecutorBuilder getExecutorBuilder(String threadExecutorClass) throws Exception {
        return (ExecutorBuilder) Class.forName(threadExecutorClass).newInstance();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
