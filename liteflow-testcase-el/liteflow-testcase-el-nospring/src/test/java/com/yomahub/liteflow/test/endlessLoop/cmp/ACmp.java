package com.yomahub.liteflow.test.endlessLoop.cmp;

import com.yomahub.liteflow.core.NodeComponent;

public class ACmp extends NodeComponent {

	@Override
	public void process() {
		System.out.println("Acomp executed!");
	}

}
