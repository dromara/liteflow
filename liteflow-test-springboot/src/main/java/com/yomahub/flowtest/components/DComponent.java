/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-8-1
 * @version 1.0
 */
package com.yomahub.flowtest.components;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.Slot;
import org.springframework.stereotype.Component;

@Component("d")
public class DComponent extends NodeComponent {

	@Override
	public void process() {
		try {
			Slot slot = this.getSlot();
			String e = slot.getOutput("e");
			if(e == null){
				System.out.println(slot);
			}
			System.out.println("D:" + slot.getOutput("e"));

			String[] temp = new String[1400];
			Thread.sleep(450L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Dcomponent executed!");

	}

}
