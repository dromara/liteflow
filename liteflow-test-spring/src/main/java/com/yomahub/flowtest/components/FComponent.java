/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.flowtest.components;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("f")
public class FComponent extends NodeComponent {

	@Override
	public void process() {
		try {
			String[] temp = new String[400];
			Thread.sleep(40L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Fcomponent executed!");

	}

}
