/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.subflow.cmp;

import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.subflow.context.TestContext;
import org.springframework.stereotype.Component;

@Component("j")
public class JCmp extends NodeComponent {

	@Override
	public void process() {
		TestContext currentContext = this.getFirstContextBean();

		String value = this.getTag();
		LiteflowResponse response = FlowExecutorHolder.loadInstance().execute2Resp("chain7_invoke", value, DefaultContext.class);
		DefaultContext subContext = response.getFirstContextBean();
		String tagValue = subContext.getData("test");

		currentContext.add2Set(tagValue);
	}
}
