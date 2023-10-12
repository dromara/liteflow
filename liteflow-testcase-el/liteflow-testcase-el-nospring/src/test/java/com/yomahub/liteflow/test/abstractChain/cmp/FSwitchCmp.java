package com.yomahub.liteflow.test.abstractChain.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;

public class FSwitchCmp extends NodeSwitchComponent {

	@Override
	public String processSwitch() throws Exception {
		return "j";
	}

}
