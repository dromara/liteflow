package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component("q")
public class QCmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) throws Exception {
		String requestData = bindCmp.getSubChainReqDataInAsync();
		DefaultContext context = bindCmp.getFirstContextBean();

		synchronized (QCmp.class) {
			if (context.hasData("test")) {
				Set<String> set = context.getData("test");
				set.add(requestData);
			}
			else {
				Set<String> set = new HashSet<>();
				set.add(requestData);
				context.setData("test", set);
			}
		}
	}

}
