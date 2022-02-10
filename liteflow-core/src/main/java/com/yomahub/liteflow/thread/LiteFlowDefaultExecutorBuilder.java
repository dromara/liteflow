package com.yomahub.liteflow.thread;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.util.SpringAware;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LiteFlow默认的并行多线程执行器实现
 * @author Bryan.Zhang
 * @since 2.6.6
 */
public class LiteFlowDefaultExecutorBuilder implements ExecutorBuilder{
    @Override
    public ExecutorService buildExecutor() {
        LiteflowConfig liteflowConfig = SpringAware.getBean(LiteflowConfig.class);
        //只有在非spring的场景下liteflowConfig才会为null
        if (ObjectUtil.isNull(liteflowConfig)){
            liteflowConfig = new LiteflowConfig();
        }
        return buildDefaultExecutor(
                liteflowConfig.getWhenMaxWorkers(),
                liteflowConfig.getWhenMaxWorkers(),
                liteflowConfig.getWhenQueueLimit(),
                "lf-when-thead-");
    }
}
