package com.yomahub.liteflow.test.multiContext.cmp;

import cn.hutool.core.date.DateUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.test.multiContext.CheckContext;
import com.yomahub.liteflow.test.multiContext.OrderContext;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		CheckContext checkContext = bindCmp.getContextBean(CheckContext.class);
		checkContext.setSign("987XYZ");
		checkContext.setRandomId(95);
		System.out.println("ACmp executed!");

	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) {
		// getContextBean无参方法是获取到第一个上下文
		OrderContext orderContext = bindCmp.getFirstContextBean();
		orderContext.setOrderNo("SO12345");
		System.out.println("BCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {
		OrderContext orderContext = bindCmp.getContextBean(OrderContext.class);
		orderContext.setOrderType(2);
		System.out.println("CCmp executed!");
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		OrderContext orderContext = bindCmp.getContextBean(OrderContext.class);
		orderContext.setCreateTime(DateUtil.parseDate("2022-06-15"));
		System.out.println("CCmp executed!");
	}

}
