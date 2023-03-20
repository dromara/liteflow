package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowELDeclSpringbootTest.RUN_TIME_SLOT;

@Component("h")
public class HCmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) throws Exception {
		String requestData = bindCmp.getSubChainReqData();
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("innerRequest", requestData);

		RUN_TIME_SLOT.add(bindCmp.getSlot().getRequestId());

		System.out.println("Hcomp executed!");
	}

}
