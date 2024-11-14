package com.yomahub.liteflow.test.chainThreadPool.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.slot.DefaultContext;

import java.util.Iterator;
import java.util.List;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {

		System.out.println("ACmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("threadName", Thread.currentThread().getName());
		System.out.println("BCmp executed!");
	}


	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		String key = "test";
		if (context.hasData(key)) {
			int count = context.getData(key);
			context.setData(key, ++count);
		} else {
			context.setData(key, 1);
		}
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "f")
	public void processE(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("threadName", Thread.currentThread().getName());
		System.out.println("FCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "i")
	public void processI(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("threadName", Thread.currentThread().getName());
		System.out.println("ICmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_ITERATOR, nodeId = "it", nodeType = NodeTypeEnum.ITERATOR)
	public Iterator<String> processIT(NodeComponent bindCmp) {
		List<String> list = bindCmp.getRequestData();
		return list.iterator();
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "w")
	public void processW(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("threadName", Thread.currentThread().getName());
		System.out.println("WCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_BOOLEAN, nodeId = "z", nodeType = NodeTypeEnum.BOOLEAN)
	public boolean processZ(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		String key = "test";
		if (context.hasData(key)) {
			int count = context.getData("test");
			return count < 5;
		} else {
			return true;
		}
	}

}
