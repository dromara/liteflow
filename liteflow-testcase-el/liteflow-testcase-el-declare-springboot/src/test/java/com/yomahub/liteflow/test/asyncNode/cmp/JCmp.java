package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import org.springframework.stereotype.Component;

@Component("j")
public class JCmp {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeType = NodeTypeEnum.SWITCH)
	public String processSwitch(NodeComponent bindCmp) throws Exception {
		System.out.println("Jcomp executed!");
		return "chain3";
	}

}
