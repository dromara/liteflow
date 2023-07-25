package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.NodeForComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import org.springframework.stereotype.Component;

@Component("x")
public class XCmp extends NodeForComponent {

	@Override
	public int processFor() throws Exception {
		return 3;
	}

}
