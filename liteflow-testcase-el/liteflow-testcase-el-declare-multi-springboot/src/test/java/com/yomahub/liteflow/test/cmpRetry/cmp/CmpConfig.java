package com.yomahub.liteflow.test.cmpRetry.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.annotation.LiteflowRetry;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		System.out.println("ACmp executed!");
	}

	private int flag = 0;

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		System.out.println("BCmp executed!");
		if (flag < 2) {
			flag++;
			throw new RuntimeException("demo exception");
		}
	}

	@LiteflowRetry(5)
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {
		System.out.println("CCmp executed!");
		throw new RuntimeException("demo exception");
	}

	@LiteflowRetry(retry = 5, forExceptions = { NullPointerException.class })
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		System.out.println("DCmp executed!");
		throw new RuntimeException("demo exception");
	}

	@LiteflowRetry(retry = 5, forExceptions = { NullPointerException.class })
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "e")
	public void processE(NodeComponent bindCmp) {
		System.out.println("ECmp executed!");
		throw new NullPointerException("demo null exception");
	}

	@LiteflowRetry(retry = 5, forExceptions = { NullPointerException.class })
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "f")
	public void processF(NodeComponent bindCmp) {
		System.out.println("ECmp executed!");
		throw new NullPointerException("demo null exception");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.AFTER_PROCESS, nodeId = "f")
	public void after(NodeComponent bindCmp) {
		System.out.println("ECmp executed!");
		throw new NullPointerException("demo null exception");
	}

}
