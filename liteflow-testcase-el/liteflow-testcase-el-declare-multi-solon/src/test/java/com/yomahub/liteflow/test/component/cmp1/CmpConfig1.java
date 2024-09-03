package com.yomahub.liteflow.test.component.cmp1;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Objects;

@LiteflowComponent
public class CmpConfig1 {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		System.out.println("AComp executed!");
		bindCmp.getSlot().setResponseData("AComp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.IS_ACCESS, nodeId = "a")
	public boolean isAccessA(NodeComponent bindCmp) {
		Integer requestData = bindCmp.getRequestData();
		if (Objects.nonNull(requestData) && requestData > 100) {
			return true;
		}
		System.out.println("AComp isAccess false.");
		return false;
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		System.out.println("BComp executed!");
		Integer requestData = bindCmp.getRequestData();
		Integer divisor = 130;
		Integer result = divisor / requestData;
		bindCmp.getSlot().setResponseData(result);
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.IS_ACCESS, nodeId = "b")
	public boolean isAccessB(NodeComponent bindCmp) {
		Integer requestData = bindCmp.getRequestData();
		if (Objects.nonNull(requestData)) {
			return true;
		}
		return false;
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {
		System.out.println("CComp executed!");
		Integer requestData = bindCmp.getRequestData();
		Integer divisor = 130;
		Integer result = divisor / requestData;
		bindCmp.getSlot().setResponseData(result);
		System.out.println("responseData=" + Integer.parseInt(bindCmp.getSlot().getResponseData()));
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.IS_CONTINUE_ON_ERROR, nodeId = "c")
	public boolean isContinueOnErrorC(NodeComponent bindCmp) {
		Integer requestData = bindCmp.getRequestData();
		if (Objects.nonNull(requestData)) {
			return true;
		}
		return false;
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) throws Exception {
		System.out.println("DComp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.IS_END, nodeId = "d")
	public boolean isEndD(NodeComponent bindCmp) {
		// 组件的process执行完之后才会执行isEnd
		Object requestData = bindCmp.getSlot().getResponseData();
		if (Objects.isNull(requestData)) {
			System.out.println("DComp flow isEnd, because of responseData is null.");
			return true;
		}
		return false;
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "e")
	public void processE(NodeComponent bindCmp) throws Exception {
		System.out.println("EComp executed!");
		Object responseData = bindCmp.getSlot().getResponseData();
		if (Objects.isNull(responseData)) {
			System.out.println("EComp responseData flow must be set end .");
			// 执行到某个条件时，手动结束流程。
			bindCmp.setIsEnd(true);
		}
		System.out.println("EComp responseData responseData=" + JsonUtil.toJsonString(responseData));
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "g")
	public void processF(NodeComponent bindCmp) {
		System.out.println("GCmp executed!");
		bindCmp.setIsEnd(true);
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.IS_CONTINUE_ON_ERROR, nodeId = "g")
	public boolean isContinueOnError(NodeComponent bindCmp) {
		return true;
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "h")
	public void processH(NodeComponent bindCmp) {
		System.out.println("HCmp executed!");
	}

}
