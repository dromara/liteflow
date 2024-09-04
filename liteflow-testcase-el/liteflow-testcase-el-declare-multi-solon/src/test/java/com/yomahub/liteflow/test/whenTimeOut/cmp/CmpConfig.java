package com.yomahub.liteflow.test.whenTimeOut.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		System.out.println("ACmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		try {
			Thread.sleep(4000);
		}
		catch (Exception ignored) {

		}
		System.out.println("BCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {
		try {
			Thread.sleep(3500);
		}
		catch (Exception ignored) {

		}
		System.out.println("CCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		try {
			Thread.sleep(4000);
		}
		catch (Exception ignored) {

		}
		System.out.println("DCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "e")
	public void processE(NodeComponent bindCmp) {
		try {
			Thread.sleep(4000);
		}
		catch (Exception ignored) {

		}
		System.out.println("ECmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "f")
	public void process(NodeComponent bindCmp) {
		try {
			Thread.sleep(4000);
		}
		catch (Exception ignored) {

		}
		System.out.println("FCmp executed!");
	}

}
