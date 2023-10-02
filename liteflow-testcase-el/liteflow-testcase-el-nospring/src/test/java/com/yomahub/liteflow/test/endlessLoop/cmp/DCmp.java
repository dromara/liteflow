package com.yomahub.liteflow.test.endlessLoop.cmp;

import com.yomahub.liteflow.core.NodeComponent;

public class DCmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		System.out.println("Dcomp executed!");
	}

}
