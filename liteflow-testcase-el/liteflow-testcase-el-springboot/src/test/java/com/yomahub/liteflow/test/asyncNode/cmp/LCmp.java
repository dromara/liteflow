package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("l")
public class LCmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		System.out.println("Lcomp executed! Throw exception");
		int i = 1/0;
	}

}
