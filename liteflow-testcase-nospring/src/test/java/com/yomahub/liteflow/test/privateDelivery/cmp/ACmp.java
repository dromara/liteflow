/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.privateDelivery.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.slot.Slot;

import java.util.HashSet;

public class ACmp extends NodeComponent {

	@Override
	public void process() {
		System.out.println("ACmp executed!");
		DefaultContext context = this.getContextBean();
		context.setData("testSet", new HashSet<>());

		for (int i = 0; i < 100; i++) {
			this.sendPrivateDeliveryData("b",i+1);
		}
	}
}

