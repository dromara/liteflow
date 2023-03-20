package com.yomahub.liteflow.test.nodeExecutor.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.annotation.LiteflowRetry;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.flow.executor.NodeExecutor;
import com.yomahub.liteflow.test.nodeExecutor.CustomerNodeExecutor;
import com.yomahub.liteflow.test.nodeExecutor.CustomerNodeExecutorAndCustomRetry;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		System.out.println("ACmp executed!");
	}

	///////////////////
	private int flag = 0;

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		System.out.println("BCmp executed!");
		if (flag < 2) {
			flag++;
			throw new RuntimeException("demo exception");
		}
	}
	///////////////////

	@LiteflowRetry(5)
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {
		System.out.println("CCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.GET_NODE_EXECUTOR_CLASS, nodeId = "c")
	public Class<? extends NodeExecutor> getNodeExecutorClassC(NodeComponent bindCmp) {
		return CustomerNodeExecutor.class;
	}

	///////////////////
	@LiteflowRetry(retry = 5, forExceptions = { NullPointerException.class })
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		System.out.println("DCmp executed!");
		throw new NullPointerException("demo exception");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.GET_NODE_EXECUTOR_CLASS, nodeId = "d")
	public Class<? extends NodeExecutor> getNodeExecutorClassD(NodeComponent bindCmp) {
		return CustomerNodeExecutorAndCustomRetry.class;
	}

}
