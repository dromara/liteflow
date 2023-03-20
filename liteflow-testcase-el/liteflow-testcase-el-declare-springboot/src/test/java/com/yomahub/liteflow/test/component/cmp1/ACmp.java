package com.yomahub.liteflow.test.component.cmp1;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("a")
public class ACmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) {
		System.out.println("AComp executed!");
		bindCmp.getSlot().setResponseData("AComp executed!");
	}

	@LiteflowMethod(LiteFlowMethodEnum.IS_ACCESS)
	public boolean isAccess(NodeComponent bindCmp) {
		Integer requestData = bindCmp.getRequestData();
		if (Objects.nonNull(requestData) && requestData > 100) {
			return true;
		}
		System.out.println("AComp isAccess false.");
		return false;
	}

}
