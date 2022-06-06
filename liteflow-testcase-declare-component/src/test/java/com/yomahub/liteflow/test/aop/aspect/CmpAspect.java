package com.yomahub.liteflow.test.aop.aspect;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.slot.Slot;

public class CmpAspect implements ICmpAroundAspect<DefaultContext> {
    @Override
    public void beforeProcess(String nodeId, Slot<DefaultContext> slot) {
        DefaultContext context = slot.getContextBean();
        context.setData(nodeId, "before");
    }

    @Override
    public void afterProcess(String nodeId, Slot<DefaultContext> slot) {
        DefaultContext context = slot.getContextBean();
        context.setData(nodeId, StrUtil.format("{}_{}", context.getData(nodeId), "after"));
    }
}
