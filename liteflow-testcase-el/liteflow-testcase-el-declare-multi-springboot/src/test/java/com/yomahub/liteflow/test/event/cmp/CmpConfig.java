package com.yomahub.liteflow.test.event.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("test", "");
		System.out.println("ACmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.ON_SUCCESS, nodeId = "a")
	public void onSuccessA(NodeComponent bindCmp) throws Exception {
		DefaultContext context = bindCmp.getFirstContextBean();
		String str = context.getData("test");
		str += bindCmp.getNodeId();
		context.setData("test", str);
	}

	////////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		System.out.println("BCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.ON_SUCCESS, nodeId = "b")
	public void onSuccessB(NodeComponent bindCmp) throws Exception {
		DefaultContext context = bindCmp.getFirstContextBean();
		String str = context.getData("test");
		str += bindCmp.getNodeId();
		context.setData("test", str);
	}

	///////////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {
		System.out.println("CCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.ON_SUCCESS, nodeId = "c")
	public void onSuccessC(NodeComponent bindCmp) throws Exception {
		DefaultContext context = bindCmp.getFirstContextBean();
		String str = context.getData("test");
		str += bindCmp.getNodeId();
		context.setData("test", str);
	}

	///////////////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) throws Exception {
		System.out.println("CCmp executed!");
		throw new NullPointerException();
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.ON_ERROR, nodeId = "d")
	public void onErrorD(NodeComponent bindCmp, Exception e) throws Exception {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("error", "error:" + bindCmp.getNodeId());
	}

	///////////////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "e")
	public void processE(NodeComponent bindCmp) throws Exception {
		System.out.println("CCmp executed!");
		throw new NullPointerException();
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.ON_ERROR, nodeId = "e")
	public void onErrorE(NodeComponent bindCmp,Exception e) throws Exception {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("error", "error:" + bindCmp.getNodeId());
		throw new IllegalAccessException("错误事件回调本身抛出异常");
	}

}
