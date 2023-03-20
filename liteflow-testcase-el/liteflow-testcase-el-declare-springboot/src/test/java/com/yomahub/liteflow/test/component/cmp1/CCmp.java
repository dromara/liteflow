package com.yomahub.liteflow.test.component.cmp1;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("c")
public class CCmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) {
		System.out.println("CComp executed!");
		Integer requestData = bindCmp.getRequestData();
		Integer divisor = 130;
		Integer result = divisor / requestData;
		bindCmp.getSlot().setResponseData(result);
		System.out.println("responseData=" + Integer.parseInt(bindCmp.getSlot().getResponseData()));
	}

	@LiteflowMethod(LiteFlowMethodEnum.IS_CONTINUE_ON_ERROR)
	public boolean isContinueOnError(NodeComponent bindCmp) {
		Integer requestData = bindCmp.getRequestData();
		if (Objects.nonNull(requestData)) {
			return true;
		}
		return false;
	}

}
