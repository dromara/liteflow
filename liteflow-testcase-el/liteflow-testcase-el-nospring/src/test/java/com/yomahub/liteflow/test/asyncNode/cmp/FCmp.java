package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeComponent;

public class FCmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		System.out.println("Fcomp executed!");
	}

}
