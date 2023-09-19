package com.yomahub.liteflow.test.abstractChain.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;

@Component("j")
public class JCmp extends NodeComponent {

	@Override
	public void process() {
		System.out.println("JCmp executed!");
	}

}
