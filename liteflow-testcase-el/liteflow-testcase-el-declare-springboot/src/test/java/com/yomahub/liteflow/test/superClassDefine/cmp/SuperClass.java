package com.yomahub.liteflow.test.superClassDefine.cmp;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;

public abstract class SuperClass {

	@LiteflowMethod(LiteFlowMethodEnum.IS_ACCESS)
	public boolean isAccess(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("isAccess", true);
		return true;
	}

}
