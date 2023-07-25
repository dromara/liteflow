package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

@LiteflowComponent("x")
public class XCmp {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_FOR, nodeType = NodeTypeEnum.FOR)
	public int processFor(NodeComponent bindCmp) throws Exception {
		return 3;
	}

}
