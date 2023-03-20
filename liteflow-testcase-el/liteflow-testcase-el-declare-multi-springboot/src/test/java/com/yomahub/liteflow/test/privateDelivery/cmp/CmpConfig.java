package com.yomahub.liteflow.test.privateDelivery.cmp;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		System.out.println("ACmp executed!");
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("testSet", new ConcurrentHashSet<>());

		for (int i = 0; i < 100; i++) {
			bindCmp.sendPrivateDeliveryData("b", i + 1);
		}
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		System.out.println("BCmp executed!");
		Integer value = bindCmp.getPrivateDeliveryData();
		DefaultContext context = bindCmp.getFirstContextBean();
		ConcurrentHashSet<Integer> testSet = context.getData("testSet");
		testSet.add(value);
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {
		System.out.println("CCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		System.out.println("CCmp executed!");
	}

}
