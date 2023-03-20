package com.yomahub.liteflow.test.execute2Future.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		System.out.println("ACmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		System.out.println("BCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {

		System.out.println("CCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		System.out.println("CCmp executed!");
	}

}
