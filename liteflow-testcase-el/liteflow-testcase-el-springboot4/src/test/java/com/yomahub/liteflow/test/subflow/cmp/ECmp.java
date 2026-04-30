/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.subflow.cmp;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("e")
public class ECmp extends NodeComponent {

	@Override
	public void process() {
		DefaultContext context = this.getFirstContextBean();

		Set<String> set;
		if (!context.hasData("set")){
			set = new ConcurrentHashSet<>();
			context.setData("set",set);
		}else {
			set = context.getData("set");
		}

		for (int i = 0; i < 100; i++) {
			LiteflowResponse response = FlowExecutorHolder.loadInstance().execute2Resp("chain4","arg"+i);
			DefaultContext innerContext = response.getFirstContextBean();
			String innerFlowData = innerContext.getData("demo");
			set.add(innerFlowData);
		}
	}
}
