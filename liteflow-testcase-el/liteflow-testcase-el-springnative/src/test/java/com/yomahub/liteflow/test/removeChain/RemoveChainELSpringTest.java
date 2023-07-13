package com.yomahub.liteflow.test.removeChain;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/removeChain/application.xml")
public class RemoveChainELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testRemoveChain() {
		LiteflowResponse response1 = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response1.isSuccess());
		FlowBus.removeChain("chain1");
		LiteflowResponse response2 = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertFalse(response2.isSuccess());
	}

}
