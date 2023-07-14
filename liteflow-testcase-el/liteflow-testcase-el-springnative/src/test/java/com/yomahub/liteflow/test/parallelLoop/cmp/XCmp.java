package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.core.NodeForComponent;
import org.springframework.stereotype.Component;

@Component("x")
public class XCmp extends NodeForComponent {

	@Override
	public int processFor() throws Exception {
		return 3;
	}

}
