package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("p")
public class PCmp {

	@Autowired
	private FlowExecutor flowExecutor;

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) throws Exception {
		int slotIndex = bindCmp.getSlotIndex();
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
