package com.yomahub.liteflow.test.sql.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("d")
public class DCmp  extends NodeComponent {

	@Override
	public void process() {
		System.out.println("DCmp executed!");
	}
}
