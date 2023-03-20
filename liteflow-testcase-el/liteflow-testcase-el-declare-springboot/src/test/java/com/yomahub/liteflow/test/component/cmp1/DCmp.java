package com.yomahub.liteflow.test.component.cmp1;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("d")
public class DCmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) throws Exception {
		System.out.println("DComp executed!");
	}

	@LiteflowMethod(LiteFlowMethodEnum.IS_END)
	public boolean isEnd(NodeComponent bindCmp) {
		// 组件的process执行完之后才会执行isEnd
		Object requestData = bindCmp.getSlot().getResponseData();
		if (Objects.isNull(requestData)) {
			System.out.println("DComp flow isEnd, because of responseData is null.");
			return true;
		}
		return false;
	}

}
