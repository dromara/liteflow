/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.contextBean.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.contextBean.context.TestContext;
import org.springframework.stereotype.Component;

@Component("c")
public class CCmp extends NodeComponent {

	@Override
	public void process() {
		TestContext context = this.getContextBean(TestContext.class);
		context.setSkuCode("J001");
		System.out.println("CCmp executed!");
	}

}
