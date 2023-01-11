
package com.yomahub.liteflow.test.iterator.cmp;

import com.yomahub.liteflow.core.NodeBreakComponent;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("b")
public class BCmp extends NodeBreakComponent {

	@Override
	public boolean processBreak() throws Exception {
		return this.getLoopIndex() == 1;
	}
}
