package com.yomahub.liteflow.test.nodeExecutor;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.executor.NodeExecutor;
import com.yomahub.liteflow.slot.DefaultContext;

import java.util.concurrent.TimeUnit;

/**
 * 自定义节点执行器
 */
public class CustomerNodeExecutorAndCustomRetry extends NodeExecutor {
    @Override
    public void execute(NodeComponent instance) throws Exception {
        DefaultContext context = instance.getFirstContextBean();
        LOG.info("使用customerNodeExecutorAndCustomRetry进行执行");
        context.setData("customerNodeExecutorAndCustomRetry", this.getClass());
        super.execute(instance);
    }

    @Override
    protected void retry(NodeComponent instance, int currentRetryCount) throws Exception {
        TimeUnit.MICROSECONDS.sleep(20L);
        DefaultContext context = instance.getFirstContextBean();
        context.setData("retryLogic", this.getClass());
        super.retry(instance, currentRetryCount);
    }
}
