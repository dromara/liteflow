package com.yomahub.liteflow.test.flowmeta;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.flowmeta.cmp2.DCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/flowmeta/application.xml")
public class FlowMetaELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试动态添加元信息节点
	@Test
	public void testFlowMeta() {
		FlowBus.addNode("d", "d组件", NodeTypeEnum.COMMON, DCmp.class);
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a request");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>c==>d[d组件]", response.getExecuteStepStr());
	}

}
