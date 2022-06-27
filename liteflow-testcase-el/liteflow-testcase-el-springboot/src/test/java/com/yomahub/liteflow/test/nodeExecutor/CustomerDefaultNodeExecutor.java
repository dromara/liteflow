package com.yomahub.liteflow.test.nodeExecutor;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.executor.NodeExecutor;
import com.yomahub.liteflow.slot.DefaultContext;

/**
 * 自定义默认的节点执行器
 */
public class CustomerDefaultNodeExecutor extends NodeExecutor {
    @Override
    public void execute(NodeComponent instance) throws Exception {
        DefaultContext context = instance.getFirstContextBean();
        LOG.info("使用customerDefaultNodeExecutor进行执行");
        context.setData("customerDefaultNodeExecutor", this.getClass());
        super.execute(instance);
    }
}
