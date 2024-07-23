
package com.yomahub.liteflow.test.iterator.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("d")
public class DCmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		System.out.println(this.getCurrLoopObj().toString());
	}
}
