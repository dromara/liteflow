/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.cmpData.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.cmpData.TestContext;
import org.springframework.stereotype.Component;

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
