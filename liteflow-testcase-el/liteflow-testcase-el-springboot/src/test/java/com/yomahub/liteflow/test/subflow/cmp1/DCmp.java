package com.yomahub.liteflow.test.subflow.cmp1;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("d")
public class DCmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		System.out.println("Dcomp executed!");
	}

}
