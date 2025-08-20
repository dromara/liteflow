package com.yomahub.liteflow.test.implicitChain.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.implicitChain.ImplicitChainELSpringbootTest;
import org.springframework.stereotype.Component;

@Component("f")
public class FCmp extends NodeComponent {

	@Override
	public void process() throws Exception {

		ImplicitChainELSpringbootTest.RUN_TIME_SLOT.add(this.getSlot().getRequestId());

		DefaultContext context = this.getFirstContextBean();
		context.setData("innerRequestData", "inner request");

		System.out.println("Fcomp executed!");
	}

}
