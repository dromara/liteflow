package com.yomahub.liteflow.test.preAndFinally.cmp;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.slot.Slot;

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
		int i = 1 / 0;
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "f1")
	public void processF1(NodeComponent bindCmp) {
		System.out.println("Finally1Cmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "f2")
	public void processF2(NodeComponent bindCmp) {
		System.out.println("Finally2Cmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "f3")
	public void processF3(NodeComponent bindCmp) throws Exception {
		Slot slot = bindCmp.getSlot();
		DefaultContext context = slot.getFirstContextBean();
		if (ObjectUtil.isNull(slot.getException())) {
			context.setData("hasEx", false);
		}
		else {
			context.setData("hasEx", true);
		}
		System.out.println("Finally3Cmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "p1")
	public void processP1(NodeComponent bindCmp) {
		System.out.println("Pre1Cmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "p2")
	public void processP2(NodeComponent bindCmp) {
		System.out.println("Pre2Cmp executed!");
	}

}
