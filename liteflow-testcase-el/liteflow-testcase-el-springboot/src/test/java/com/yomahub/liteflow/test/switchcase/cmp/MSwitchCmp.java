package com.yomahub.liteflow.test.switchcase.cmp;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;

@LiteflowComponent("m")
public class MSwitchCmp extends NodeSwitchComponent {

	@Override
	public String processSwitch() throws Exception {
		String tag = this.getTag();
		return StrUtil.format(":{}", tag);
	}

}
