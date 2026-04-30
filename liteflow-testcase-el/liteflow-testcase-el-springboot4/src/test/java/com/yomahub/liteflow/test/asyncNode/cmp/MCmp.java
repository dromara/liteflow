package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("m")
public class MCmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		String seconds = this.getTag();
		Thread.sleep((long) (1000 * Double.parseDouble(seconds)));
		DefaultContext context = this.getFirstContextBean();
		synchronized (MCmp.class) {
			if (context.hasData("count")) {
				Integer count = context.getData("count");
				context.setData("count", ++count);
			} else {
				context.setData("count", 1);
			}
		}
		System.out.println("Mcomp executed!");
	}

}
