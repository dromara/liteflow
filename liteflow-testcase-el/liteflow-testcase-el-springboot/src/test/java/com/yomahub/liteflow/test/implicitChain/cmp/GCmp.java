package com.yomahub.liteflow.test.implicitChain.cmp;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.implicitChain.ImplicitChainELSpringbootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component("g")
public class GCmp extends NodeComponent {

	@Autowired
	private FlowExecutor flowExecutor;

	@Override
	public void process() throws Exception {

		ImplicitChainELSpringbootTest.RUN_TIME_SLOT.add(this.getSlot().getRequestId());

		System.out.println("Gcmp executed!");

		this.invoke2Resp("chain4", "it's implicit subflow.");
	}

}
