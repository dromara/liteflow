/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.useTTLInWhen.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.useTTLInWhen.TestTL;
import org.springframework.stereotype.Component;

@Component("f")
public class FCmp extends NodeComponent {

	@Override
	public void process() {
		DefaultContext context = this.getFirstContextBean();
		String value = TestTL.get();
		context.setData(this.getNodeId(), value + ",f");
		System.out.println("FCmp executed!");
	}

}
