package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowTest.RUN_TIME_SLOT;

public class HCmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		String requestData = this.getSubChainReqData();
		DefaultContext context = this.getFirstContextBean();
		context.setData("innerRequest", requestData);

		RUN_TIME_SLOT.add(this.getSlot().getRequestId());

		System.out.println("Hcomp executed!");
	}

}
