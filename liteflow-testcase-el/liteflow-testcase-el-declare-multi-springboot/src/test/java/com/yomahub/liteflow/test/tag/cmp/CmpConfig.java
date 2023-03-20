package com.yomahub.liteflow.test.tag.cmp;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		String testKey = "test";

		DefaultContext context = bindCmp.getFirstContextBean();
		if (context.getData(testKey) == null) {
			context.setData(testKey, bindCmp.getTag());
		}
		else {
			String s = context.getData(testKey);
			s += bindCmp.getTag();
			context.setData(testKey, s);
		}
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b1")
	public void processB1(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("test", new ConcurrentHashSet<String>());

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		ConcurrentHashSet<String> testSet = context.getData("test");
		testSet.add(bindCmp.getTag());

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "c", nodeType = NodeTypeEnum.SWITCH)
	public String processSwitchC(NodeComponent bindCmp) {
		if (bindCmp.getTag().equals("2")) {
			return "e";
		}
		else {
			return "d";
		}
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		System.out.println(bindCmp.getTag());
		System.out.println("ECmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "e")
	public void processE(NodeComponent bindCmp) {
		System.out.println(bindCmp.getTag());
		System.out.println("ECmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "f")
	public void processF() {
		System.out.println("FCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.IS_ACCESS, nodeId = "f")
	public boolean isAccessF(NodeComponent bindCmp) {
		return Boolean.parseBoolean(bindCmp.getTag());
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "g")
	public void process(NodeComponent bindCmp) {
		System.out.println("GCmp executed!");
	}

}
