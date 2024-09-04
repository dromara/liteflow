package com.yomahub.liteflow.test.cmpStep.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) throws Exception {
		Thread.sleep(5000L);
		System.out.println("ACmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		System.out.println("BCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) throws Exception {
		System.out.println("CCmp executed!");
		Thread.sleep(2000);
		throw new RuntimeException("test error c");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		System.out.println("CCmp executed!");
		throw new RuntimeException("test error d");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "e")
	public void processE(NodeComponent bindCmp) {
		System.out.println("ECmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.IS_ACCESS, nodeId = "e")
	public boolean isAccessE(NodeComponent bindCmp) {
		return false;
	}

}
