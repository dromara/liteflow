package com.yomahub.liteflow.test.aop.aspect;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.slot.Slot;

public class CmpAspect implements ICmpAroundAspect {

	@Override
	public void beforeProcess(String nodeId, Slot slot) {
		DefaultContext context = slot.getFirstContextBean();
		context.setData(nodeId, "before");
	}

	@Override
	public void afterProcess(String nodeId, Slot slot) {
		DefaultContext context = slot.getFirstContextBean();
		context.setData(nodeId, StrUtil.format("{}_{}", context.getData(nodeId), "after"));
	}

}
