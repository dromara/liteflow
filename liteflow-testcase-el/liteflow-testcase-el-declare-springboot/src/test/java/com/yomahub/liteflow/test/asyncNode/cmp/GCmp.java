package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;

@Component("g")
public class GCmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) throws Exception {
		Thread.sleep(500);
		DefaultContext context = bindCmp.getFirstContextBean();
		synchronized (NodeComponent.class) {
			if (context.hasData("check")) {
				String str = context.getData("check");
				str += bindCmp.getNodeId();
				context.setData("check", str);
			}
			else {
				context.setData("check", bindCmp.getNodeId());
			}
		}
		System.out.println("Gcomp executed!");
	}

}
