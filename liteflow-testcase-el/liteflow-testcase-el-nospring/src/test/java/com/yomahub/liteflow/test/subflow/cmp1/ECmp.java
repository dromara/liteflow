package com.yomahub.liteflow.test.subflow.cmp1;

import com.yomahub.liteflow.core.NodeComponent;

public class ECmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		System.out.println("Ecomp executed!");
	}

}
