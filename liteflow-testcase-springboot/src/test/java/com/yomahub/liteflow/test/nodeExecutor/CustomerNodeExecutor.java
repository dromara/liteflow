package com.yomahub.liteflow.test.nodeExecutor;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.entity.executor.NodeExecutor;

import java.util.concurrent.TimeUnit;

/**
 * 自定义节点执行器
 */
public class CustomerNodeExecutor extends NodeExecutor {
    @Override
    public void execute(NodeComponent instance) throws Exception {
        Slot slot = DataBus.getSlot(instance.getSlotIndex());
        LOG.info("使用customerNodeExecutor进行执行");
        slot.setData("customerNodeExecutor", this.getClass());
        super.execute(instance);
    }

}
