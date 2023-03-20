package com.yomahub.liteflow.test.parsecustom.cmp;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.FlowSystemException;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		String str = bindCmp.getRequestData();
		if (StrUtil.isNotBlank(str) && str.equals("exception")) {
			throw new FlowSystemException("chain execute execption");
		}
		System.out.println("ACmp executed!");
	}
	///////////////////

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		System.out.println("BCmp executed!");
	}

	///////////////////

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {
		System.out.println("CCmp executed!");
	}

	///////////////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		System.out.println("DCmp executed!");
	}

	///////////////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "e", nodeType = NodeTypeEnum.SWITCH)
	public String processSwitchE(NodeComponent bindCmp) throws Exception {
		return "g";
	}
	//////////////////

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "f")
	public void processF(NodeComponent bindCmp) {
		System.out.println("FCmp executed!");
	}

	///////////////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "g")
	public void processG(NodeComponent bindCmp) {
		System.out.println("GCmp executed!");
	}

}
