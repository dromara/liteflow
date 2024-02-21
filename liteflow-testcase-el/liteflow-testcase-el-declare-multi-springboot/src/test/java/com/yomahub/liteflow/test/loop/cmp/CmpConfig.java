package com.yomahub.liteflow.test.loop.cmp;

import cn.hutool.core.util.StrUtil;
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
		System.out.println("ACmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {

		System.out.println("BCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {
		System.out.println("CCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		String key = "test";
		if (context.hasData(key)) {
			int count = context.getData(key);
			context.setData(key, ++count);
		}
		else {
			context.setData(key, 1);
		}
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "e")
	public void processE(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		String key = StrUtil.format("{}_{}", "loop", bindCmp.getTag());
		if (context.hasData(key)) {
			String loopStr = context.getData(key);
			String loopStrReturn = StrUtil.format("{}{}", loopStr, bindCmp.getLoopIndex());
			context.setData(key, loopStrReturn);
		}
		else {
			context.setData(key, bindCmp.getLoopIndex().toString());
		}
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_FOR, nodeId = "x", nodeType = NodeTypeEnum.FOR)
	public int processX(NodeComponent bindCmp) {
		return 3;
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_BOOLEAN, nodeId = "y", nodeType = NodeTypeEnum.BOOLEAN)
	public boolean processY(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		int count = context.getData("test");
		return count > 3;
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_BOOLEAN, nodeId = "z", nodeType = NodeTypeEnum.BOOLEAN)
	public boolean processZ(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		String key = "test";
		if (context.hasData(key)) {
			int count = context.getData("test");
			return count < 5;
		}
		else {
			return true;
		}
	}

}
