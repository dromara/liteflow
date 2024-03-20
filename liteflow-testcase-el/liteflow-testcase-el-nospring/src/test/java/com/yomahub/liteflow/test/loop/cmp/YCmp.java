package com.yomahub.liteflow.test.loop.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import com.yomahub.liteflow.slot.DefaultContext;

public class YCmp extends NodeBooleanComponent {

	@Override
	public boolean processBoolean() throws Exception {
		DefaultContext context = this.getFirstContextBean();
		int count = context.getData("test");
		return count > 3;
	}

}
