package com.yomahub.liteflow.test.component.cmp1;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("b")
public class BCmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) {
		System.out.println("BComp executed!");
		Integer requestData = bindCmp.getRequestData();
		Integer divisor = 130;
		Integer result = divisor / requestData;
		bindCmp.getSlot().setResponseData(result);
	}

	@LiteflowMethod(LiteFlowMethodEnum.IS_ACCESS)
	public boolean isAccess(NodeComponent bindCmp) {
		Integer requestData = bindCmp.getRequestData();
		if (Objects.nonNull(requestData)) {
			return true;
		}
		return false;
	}

}
