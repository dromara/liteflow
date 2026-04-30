package com.yomahub.liteflow.test.loop.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent("z")
public class ZCmp extends NodeBooleanComponent {

	@Override
	public boolean processBoolean() throws Exception {
		return this.getLoopIndex()<5;
	}

}
