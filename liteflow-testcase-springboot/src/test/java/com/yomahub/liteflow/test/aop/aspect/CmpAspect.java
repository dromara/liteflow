package com.yomahub.liteflow.test.aop.aspect;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.entity.data.Slot;

public class CmpAspect implements ICmpAroundAspect {
    @Override
    public void beforeProcess(String nodeId, Slot slot) {
        slot.setData(nodeId, "before");
    }

    @Override
    public void afterProcess(String nodeId, Slot slot) {
        slot.setData(nodeId, StrUtil.format("{}_{}", slot.getData(nodeId), "after"));
    }
}
