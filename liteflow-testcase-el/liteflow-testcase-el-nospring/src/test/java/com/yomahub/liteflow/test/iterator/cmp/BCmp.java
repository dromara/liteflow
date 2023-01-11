
package com.yomahub.liteflow.test.iterator.cmp;

import com.yomahub.liteflow.core.NodeBreakComponent;

public class BCmp extends NodeBreakComponent {

	@Override
	public boolean processBreak() throws Exception {
		return this.getLoopIndex() == 1;
	}
}
