package com.yomahub.liteflow.test.parent.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.test.base.cmp.TestDomain;

import javax.annotation.Resource;

@LiteflowComponent
public class CmpConfig extends ParentClass{

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
        this.setName("jack");
        System.out.println(this.sayHi());
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
        this.setName("tom");
        System.out.println(this.sayHi());
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {
        this.setName("jerry");
        System.out.println(this.sayHi());
	}
}
