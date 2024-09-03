package com.yomahub.liteflow.test.component.cmp2;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

import java.util.Objects;

@LiteflowComponent
public class FCondCmp {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "f", nodeType = NodeTypeEnum.SWITCH)
	public String processSwitchF(NodeComponent bindCmp) {
		Integer requestData = bindCmp.getRequestData();
		if (Objects.isNull(requestData)) {
			return "d";
		}
		else if (requestData == 0) {
			return "c";
		}
		else {
			return "b";
		}
	}

}
