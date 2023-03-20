package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowELSpringbootTest.RUN_TIME_SLOT;

@Component("p")
public class PCmp extends NodeComponent {

	@Autowired
	private FlowExecutor flowExecutor;

	@Override
	public void process() throws Exception {
		int slotIndex = this.getSlotIndex();
		for (int i = 0; i < 10; i++) {
			int finalI = i;
			new Thread(() -> flowExecutor.invoke2RespInAsync("c2", "it's implicit subflow " + finalI, slotIndex))
				.start();
		}
		Thread.sleep(1000);
	}

}
