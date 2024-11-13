package com.yomahub.liteflow.thread;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;

import java.util.concurrent.ExecutorService;

/**
 * LiteFlow默认全局线程池执行器实现
 *
 * @author jason
 */
public class LiteFlowDefaultGlobalExecutorBuilder implements ExecutorBuilder {

    @Override
    public ExecutorService buildExecutor() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        // 只有在非spring的场景下liteflowConfig才会为null
        if (ObjectUtil.isNull(liteflowConfig)) {
            liteflowConfig = new LiteflowConfig();
        }
        return buildDefaultExecutor(liteflowConfig.getGlobalThreadPoolSize(), liteflowConfig.getGlobalThreadPoolSize(),
                                    liteflowConfig.getGlobalThreadPoolQueueSize(), "global-thread-");
    }

}
