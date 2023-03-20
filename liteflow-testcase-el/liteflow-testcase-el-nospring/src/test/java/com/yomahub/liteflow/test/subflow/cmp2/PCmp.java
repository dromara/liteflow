package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.core.NodeComponent;

public class PCmp extends NodeComponent {

	private FlowExecutor flowExecutor = FlowExecutorHolder.loadInstance();

	@Override
	public void process() throws Exception {
		int slotIndex = this.getSlotIndex();
		for (int i = 0; i < 10; i++) {
			int finalI = i;
			new Thread(() -> {
				try {
					flowExecutor.invokeInAsync("c2", "it's implicit subflow " + finalI, slotIndex);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}).start();
		}
		Thread.sleep(1000);
	}

}
