/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.thread;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.exception.ThreadExecutorServiceCreateException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.util.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;


/**
 * 线程池工具类
 * @author Bryan.Zhang
 */
public class ExecutorHelper {
    
    private final Logger LOG = LoggerFactory.getLogger(ExecutorHelper.class);

    private static ExecutorHelper executorHelper;

    private ExecutorService executorService;
    
    private ExecutorHelper() {
    }

    public static ExecutorHelper loadInstance(){
        if (ObjectUtil.isNull(executorHelper)){
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
        if (ObjectUtil.isNull(executorService)){
            LiteflowConfig liteflowConfig = SpringAware.getBean(LiteflowConfig.class);

            try{
                assert liteflowConfig != null;
                ExecutorBuilder executorBuilder = (ExecutorBuilder)Class.forName(liteflowConfig.getThreadExecutorClass()).newInstance();
                executorService = executorBuilder.buildExecutor();
            }catch (Exception e){
                LOG.error(e.getMessage(), e);
                throw new ThreadExecutorServiceCreateException(e.getMessage());
            }

        }
        return executorService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
