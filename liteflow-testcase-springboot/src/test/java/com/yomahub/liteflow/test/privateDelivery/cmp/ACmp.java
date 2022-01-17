/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.privateDelivery.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.Slot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@LiteflowComponent("a")
public class ACmp extends NodeComponent {
	@Autowired
	private FlowExecutor flowExecutor;
	@Override
	public void process() {
		System.out.println("ACmp executed!");
		Slot slot = getSlot();
		slot.setData("testSet", new HashSet<>());

		try {
			Queue<Integer> queue = new ConcurrentLinkedQueue<>();
			for (int i = 1; i <= 100; i++) {
				queue.add(i);
			}
            flowExecutor.execute2Resp("chain2", queue);

		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}
