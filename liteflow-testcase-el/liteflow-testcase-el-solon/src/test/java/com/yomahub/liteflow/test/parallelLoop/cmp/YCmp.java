package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.core.NodeBreakComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.noear.solon.annotation.Component;

@Component("y")
public class YCmp extends NodeBreakComponent {

	@Override
	public boolean processBreak() throws Exception {
		DefaultContext context = this.getFirstContextBean();
		int count = 0;
		if(context.hasData("test")) {
			count = context.getData("test");
		}
		return count > 3;
	}
}
