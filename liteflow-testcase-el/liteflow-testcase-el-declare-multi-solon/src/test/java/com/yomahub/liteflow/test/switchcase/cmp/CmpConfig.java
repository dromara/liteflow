package com.yomahub.liteflow.test.switchcase.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

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

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "e", nodeType = NodeTypeEnum.SWITCH)
	public String processE(NodeComponent bindCmp) {
		return "d";
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "f", nodeType = NodeTypeEnum.SWITCH)
	public String processF(NodeComponent bindCmp) {
		return ":td";
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "g", nodeType = NodeTypeEnum.SWITCH)
	public String processG(NodeComponent bindCmp) {
		return "d:td";
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "h", nodeType = NodeTypeEnum.SWITCH)
	public String processH(NodeComponent bindCmp) {
		return "tag:td";
	}

}
