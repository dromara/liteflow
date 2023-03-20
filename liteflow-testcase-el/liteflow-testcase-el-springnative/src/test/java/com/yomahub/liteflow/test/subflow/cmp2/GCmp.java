package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowELSpringTest.RUN_TIME_SLOT;

@Component("g")
public class GCmp extends NodeComponent {

	@Override
	public void process() throws Exception {

		RUN_TIME_SLOT.add(this.getSlot().getRequestId());

		System.out.println("Gcmp executed!");

		this.invoke("chain4", "it's implicit subflow.");
	}

}
