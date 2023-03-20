package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowELDeclSpringbootTest.RUN_TIME_SLOT;

@Component("g")
public class GCmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) throws Exception {

		RUN_TIME_SLOT.add(bindCmp.getSlot().getRequestId());

		System.out.println("Gcmp executed!");

		bindCmp.invoke("chain4", "it's implicit subflow.");
	}

}
