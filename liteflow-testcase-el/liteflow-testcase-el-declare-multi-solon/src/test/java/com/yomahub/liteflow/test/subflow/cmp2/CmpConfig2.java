package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import org.noear.solon.annotation.Inject;

import java.util.HashSet;
import java.util.Set;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowELDeclMultiSpringbootTest.RUN_TIME_SLOT;

@LiteflowComponent
public class CmpConfig2 {

	@Inject
	private FlowExecutor flowExecutor;

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "f")
	public void processF(NodeComponent bindCmp) {

		RUN_TIME_SLOT.add(bindCmp.getSlot().getRequestId());

		System.out.println("Fcomp executed!");

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "g")
	public void processG(NodeComponent bindCmp) throws Exception {
		RUN_TIME_SLOT.add(bindCmp.getSlot().getRequestId());

		System.out.println("Gcmp executed!");

		bindCmp.invoke("chain4", "it's implicit subflow.");

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "h")
	public void processH(NodeComponent bindCmp) {
		String requestData = bindCmp.getSubChainReqData();
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("innerRequest", requestData);

		RUN_TIME_SLOT.add(bindCmp.getSlot().getRequestId());

		System.out.println("Hcomp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "m")
	public void processM(NodeComponent bindCmp) {
		RUN_TIME_SLOT.add(bindCmp.getSlot().getRequestId());

		System.out.println("Mcomp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "p")
	public void processP(NodeComponent bindCmp) throws Exception {
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

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "q")
	public void processQ(NodeComponent bindCmp) throws Exception {
		String requestData = bindCmp.getSubChainReqDataInAsync();
		DefaultContext context = bindCmp.getFirstContextBean();

		synchronized (this) {
			if (context.hasData("test")) {
				Set<String> set = context.getData("test");
				set.add(requestData);
			}
			else {
				Set<String> set = new HashSet<>();
				set.add(requestData);
				context.setData("test", set);
			}
		}
	}

}
