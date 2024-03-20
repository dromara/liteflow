package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("y")
public class YCmp extends NodeBooleanComponent {

	@Override
	public boolean processBoolean() throws Exception {
		DefaultContext context = this.getFirstContextBean();
		int count = 0;
		if(context.hasData("test")) {
			count = context.getData("test");
		}
		return count > 3;
	}
}
