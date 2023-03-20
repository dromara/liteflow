package com.yomahub.liteflow.test.component.cmp1;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.util.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("e")
public class ECmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) throws Exception {
		System.out.println("EComp executed!");
		Object responseData = bindCmp.getSlot().getResponseData();
		if (Objects.isNull(responseData)) {
			System.out.println("EComp responseData flow must be set end .");
			// 执行到某个条件时，手动结束流程。
			bindCmp.setIsEnd(true);
		}
		System.out.println("EComp responseData responseData=" + JsonUtil.toJsonString(responseData));
	}

}
