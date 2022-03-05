package com.yomahub.liteflow.test.nodeExecutor;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.entity.executor.NodeExecutor;

/**
 * 自定义默认的节点执行器
 */
public class CustomerDefaultNodeExecutor extends NodeExecutor {
    @Override
    public void execute(NodeComponent instance) throws Exception {
        Slot slot = DataBus.getSlot(instance.getSlotIndex());
        LOG.info("使用customerDefaultNodeExecutor进行执行");
        slot.setData("customerDefaultNodeExecutor", this.getClass());
        super.execute(instance);
    }
}
