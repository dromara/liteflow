package com.yomahub.liteflow.test.endlessLoop.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("e")
public class ECmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		System.out.println("Ecomp executed!");
	}

}
