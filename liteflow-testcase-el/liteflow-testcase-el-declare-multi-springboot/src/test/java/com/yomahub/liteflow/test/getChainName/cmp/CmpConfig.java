package com.yomahub.liteflow.test.getChainName.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData(bindCmp.getNodeId(), bindCmp.getCurrChainId());
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData(bindCmp.getNodeId(), bindCmp.getCurrChainId());
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData(bindCmp.getNodeId(), bindCmp.getCurrChainId());
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData(bindCmp.getNodeId(), bindCmp.getCurrChainId());
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "e")
	public void processE(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		if (context.hasData(bindCmp.getNodeId())) {
			context.setData(bindCmp.getNodeId(), context.getData(bindCmp.getNodeId()) + "_" + bindCmp.getCurrChainId());
		}
		else {
			context.setData(bindCmp.getNodeId(), bindCmp.getCurrChainId());
		}
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "f")
	public void processF(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData(bindCmp.getNodeId(), bindCmp.getCurrChainId());
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "g")
	public void processG(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData(bindCmp.getNodeId(), bindCmp.getCurrChainId());
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "h", nodeType = NodeTypeEnum.SWITCH)
	public String processSwitchH(NodeComponent bindCmp) throws Exception {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData(bindCmp.getNodeId(), bindCmp.getCurrChainId());
		return "j";
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "j")
	public void processJ(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData(bindCmp.getNodeId(), bindCmp.getCurrChainId());
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "k")
	public void process(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData(bindCmp.getNodeId(), bindCmp.getCurrChainId());
	}

}
