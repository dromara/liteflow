package com.yomahub.liteflow.test.subflow.cmp1;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("c")
public class CCmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		System.out.println("Ccomp executed!");
	}

}
