/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.customWhenThreadPool.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("e")
public class ECmp extends NodeComponent {

	@Override
	public void process() {
		this.getSlot().setData("threadName", Thread.currentThread().getName());
		System.out.println("ECmp executed!");
	}

}
