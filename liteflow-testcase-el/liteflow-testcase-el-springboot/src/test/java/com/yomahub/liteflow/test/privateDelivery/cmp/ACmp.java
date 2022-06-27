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
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent("a")
public class ACmp extends NodeComponent {

	@Override
	public void process() {
		System.out.println("ACmp executed!");
		DefaultContext context = this.getFirstContextBean();
		context.setData("testSet", new ConcurrentHashSet<>());

		for (int i = 0; i < 100; i++) {
			this.sendPrivateDeliveryData("b",i+1);
		}
	}
}

