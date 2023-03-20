package com.yomahub.liteflow.test.subflow.cmp1;

import com.yomahub.liteflow.core.NodeComponent;

public class ACmp extends NodeComponent {

	@Override
	public void process() {
		System.out.println("Acomp executed!");
	}

}
