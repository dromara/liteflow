package com.yomahub.liteflow.test.base.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

import javax.annotation.Resource;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a", nodeName = "A组件")
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

	@Resource
	private TestDomain testDomain;

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		testDomain.sayHi();
		System.out.println("CCmp executed!");
	}


	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "e")
	public void processE(NodeComponent bindCmp) {
		System.out.println("ECmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.BEFORE_PROCESS, nodeId = "e")
	public void beforeProcessE(NodeComponent bindCmp) {
		int a = 1/0;
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.ON_ERROR, nodeId = "e")
	public void onErrorE(NodeComponent bindCmp, Exception e) {
		if (e != null){
			e.printStackTrace();
		}else{
			System.out.println("no error");
		}
	}
}
