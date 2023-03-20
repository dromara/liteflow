package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.test.asyncNode.exception.TestException;
import org.springframework.stereotype.Component;

@Component("i")
public class ICmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) throws Exception {
		DefaultContext context = bindCmp.getFirstContextBean();
		synchronized (ICmp.class) {
			if (context.hasData("count")) {
				Integer count = context.getData("count");
				context.setData("count", ++count);
			}
			else {
				context.setData("count", 1);
			}
		}
		System.out.println("Icomp executed! throw Exception!");
		throw new TestException();
	}

}
