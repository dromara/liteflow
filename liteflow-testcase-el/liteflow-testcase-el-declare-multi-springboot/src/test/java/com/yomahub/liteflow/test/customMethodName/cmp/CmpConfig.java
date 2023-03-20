package com.yomahub.liteflow.test.customMethodName.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.Slot;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processAcmp(NodeComponent bindCmp) {
		System.out.println("ACmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.IS_ACCESS, nodeId = "a")
	public boolean isAcmpAccess(NodeComponent bindCmp) {
		return true;
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.BEFORE_PROCESS, nodeId = "a")
	public void beforeAcmp(NodeComponent bindCmp) {
		System.out.println("before A");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.AFTER_PROCESS, nodeId = "a")
	public void afterAcmp(NodeComponent bindCmp) {
		System.out.println("after A");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.ON_SUCCESS, nodeId = "a")
	public void onAcmpSuccess(NodeComponent bindCmp) {
		System.out.println("Acmp success");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.ON_ERROR, nodeId = "a")
	public void onAcmpError(NodeComponent bindCmp) {
		System.out.println("Acmp error");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.IS_END, nodeId = "a")
	public boolean isAcmpEnd(NodeComponent bindCmp) {
		System.out.println("Acmp end config");
		return false;
	}

	///////////////////////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processBcmp(NodeComponent bindCmp) {
		System.out.println("BCmp executed!");
	}

}
