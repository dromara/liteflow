package com.yomahub.liteflow.test.builder.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.builder.TestContext;
import org.springframework.stereotype.Component;

/**
 * EL表达式装配并执行测试
 *
 * @author gezuao
 * @since 2.11.1
 */
@Component("c")
public class CCmp extends NodeComponent {

	@Override
	public void process() {
		String data = this.getCmpData(String.class);
		TestContext context = this.getFirstContextBean();
		context.add2Set(data);
		System.out.println("CCmp executed!");
	}

}
