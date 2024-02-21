package com.yomahub.liteflow.test.loop.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import com.yomahub.liteflow.core.NodeForComponent;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent("y")
public class YCmp extends NodeBooleanComponent {

	@Override
	public boolean processBoolean() throws Exception {
		return this.getLoopIndex() > 2;
	}

}
