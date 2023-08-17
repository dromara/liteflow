package com.yomahub.liteflow.test.aop.aspect;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.slot.Slot;

public class CmpAspect implements ICmpAroundAspect {

	@Override
	public void beforeProcess(NodeComponent cmp) {
		DefaultContext context = cmp.getFirstContextBean();
		context.setData(cmp.getNodeId(), "before");
	}

	@Override
	public void afterProcess(NodeComponent cmp) {
		DefaultContext context = cmp.getFirstContextBean();
		context.setData(cmp.getNodeId(), StrUtil.format("{}_{}", context.getData(cmp.getNodeId()), "after"));
	}

	@Override
	public void onSuccess(NodeComponent cmp) {

	}

	@Override
	public void onError(NodeComponent cmp, Exception e) {
		DefaultContext context = cmp.getFirstContextBean();
		context.setData(cmp.getNodeId()+"_error", e.getMessage());
	}

}
