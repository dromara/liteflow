package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.asyncNode.exception.TestException;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		synchronized (NodeComponent.class) {
			if (context.hasData("check")) {
				String str = context.getData("check");
				str += bindCmp.getNodeId();
				context.setData("check", str);
			}
			else {
				context.setData("check", bindCmp.getNodeId());
			}
		}
		System.out.println("Acomp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		synchronized (NodeComponent.class) {
			if (context.hasData("check")) {
				String str = context.getData("check");
				str += bindCmp.getNodeId();
				context.setData("check", str);
			}
			else {
				context.setData("check", bindCmp.getNodeId());
			}
		}
		System.out.println("Bcomp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) throws Exception {
		DefaultContext context = bindCmp.getFirstContextBean();
		synchronized (NodeComponent.class) {
			if (context.hasData("check")) {
				String str = context.getData("check");
				str += bindCmp.getNodeId();
				context.setData("check", str);
			}
			else {
				context.setData("check", bindCmp.getNodeId());
			}
		}
		System.out.println("Ccomp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) throws Exception {
		Thread.sleep(1000);
		DefaultContext context = bindCmp.getFirstContextBean();
		synchronized (NodeComponent.class) {
			if (context.hasData("check")) {
				String str = context.getData("check");
				str += bindCmp.getNodeId();
				context.setData("check", str);
			}
			else {
				context.setData("check", bindCmp.getNodeId());
			}
		}
		System.out.println("Dcomp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "e", nodeType = NodeTypeEnum.SWITCH)
	public String processSwitchE(NodeComponent bindCmp) throws Exception {
		System.out.println("Ecomp executed!");
		return "g";
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "f")
	public void processF(NodeComponent bindCmp) throws Exception {
		System.out.println("Fcomp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "g")
	public void processG(NodeComponent bindCmp) throws Exception {
		Thread.sleep(500);
		DefaultContext context = bindCmp.getFirstContextBean();
		synchronized (NodeComponent.class) {
			if (context.hasData("check")) {
				String str = context.getData("check");
				str += bindCmp.getNodeId();
				context.setData("check", str);
			}
			else {
				context.setData("check", bindCmp.getNodeId());
			}
		}
		System.out.println("Gcomp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "h")
	public void processH(NodeComponent bindCmp) throws Exception {
		DefaultContext context = bindCmp.getFirstContextBean();
		synchronized (NodeComponent.class) {
			if (context.hasData("check")) {
				String str = context.getData("check");
				str += bindCmp.getNodeId();
				context.setData("check", str);
			}
			else {
				context.setData("check", bindCmp.getNodeId());
			}

		}

		System.out.println("Hcomp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "i")
	public void processI(NodeComponent bindCmp) throws Exception {
		DefaultContext context = bindCmp.getFirstContextBean();
		synchronized (this) {
			if (context.hasData("count")) {
				Integer count = context.getData("count");
				context.setData("count", ++count);
			}
			else {
				context.setData("count", 1);
			}
		}
		System.out.println("Icomp executed! throw Exception!");
		throw new TestException();
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "j", nodeType = NodeTypeEnum.SWITCH)
	public String processSwitchJ(NodeComponent bindCmp) throws Exception {
		System.out.println("Jcomp executed!");
		return "chain3";
	}

}
