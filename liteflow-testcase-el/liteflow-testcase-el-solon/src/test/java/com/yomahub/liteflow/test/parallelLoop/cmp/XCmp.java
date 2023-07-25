package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.core.NodeForComponent;
import org.noear.solon.annotation.Component;

@Component("x")
public class XCmp extends NodeForComponent {

	@Override
	public int processFor() throws Exception {
		return 3;
	}

}
