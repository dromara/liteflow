package com.yomahub.liteflow.test.implicitChain.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.implicitChain.ImplicitChainELSpringbootTest;
import org.springframework.stereotype.Component;


@Component("m")
public class MCmp extends NodeComponent {

	@Override
	public void process() throws Exception {

		ImplicitChainELSpringbootTest.RUN_TIME_SLOT.add(this.getSlot().getRequestId());

		System.out.println("Mcomp executed!");
	}

}
