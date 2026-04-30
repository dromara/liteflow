package com.yomahub.liteflow.test.implicitChain.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.implicitChain.ImplicitChainELSpringbootTest;
import org.springframework.stereotype.Component;


@Component("h")
public class HCmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		String requestData = this.getRequestData();
		DefaultContext context = this.getFirstContextBean();
		context.setData("innerRequest", requestData);

		ImplicitChainELSpringbootTest.RUN_TIME_SLOT.add(this.getSlot().getRequestId());

		System.out.println("Hcomp executed!");
	}

}
