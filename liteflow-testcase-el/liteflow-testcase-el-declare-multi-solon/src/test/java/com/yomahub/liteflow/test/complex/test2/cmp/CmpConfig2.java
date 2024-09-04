package com.yomahub.liteflow.test.complex.test2.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

@LiteflowComponent
public class CmpConfig2 {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "A")
	public void processA(NodeComponent bindCmp) {

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeType = NodeTypeEnum.SWITCH, nodeId = "B")
	public String processSwitchB(NodeComponent bindCmp) {
		return "t3";
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "C")
	public void processC(NodeComponent bindCmp) {

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "D")
	public void processD(NodeComponent bindCmp) {

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "E")
	public void processE(NodeComponent bindCmp) {

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "F")
	public void processF(NodeComponent bindCmp) {

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeType = NodeTypeEnum.SWITCH, nodeId = "G")
	public String processSwitchG(NodeComponent bindCmp) throws Exception {
		return "t2";
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "H")
	public void processH(NodeComponent bindCmp) {

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "J")
	public void processJ(NodeComponent bindCmp) {

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "K")
	public void processK(NodeComponent bindCmp) {

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "L")
	public void processL(NodeComponent bindCmp) {

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "M")
	public void processM(NodeComponent bindCmp) {

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "N")
	public void processN(NodeComponent bindCmp) {

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "Z")
	public void processZ(NodeComponent bindCmp) {

	}

}
