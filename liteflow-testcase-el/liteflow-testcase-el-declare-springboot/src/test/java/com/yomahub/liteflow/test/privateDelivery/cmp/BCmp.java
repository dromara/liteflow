/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.privateDelivery.cmp;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent("b")
public class BCmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) {
		System.out.println("BCmp executed!");
		Integer value = bindCmp.getPrivateDeliveryData();
		DefaultContext context = bindCmp.getFirstContextBean();
		ConcurrentHashSet<Integer> testSet = context.getData("testSet");
		testSet.add(value);
	}

}
