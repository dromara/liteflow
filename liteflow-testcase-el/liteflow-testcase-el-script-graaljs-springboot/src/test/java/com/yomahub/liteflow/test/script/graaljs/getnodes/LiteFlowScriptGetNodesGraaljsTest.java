package com.yomahub.liteflow.test.script.graaljs.getnodes;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.List;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/getnodes/application.properties")
@SpringBootTest(classes = LiteFlowScriptGetNodesGraaljsTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.script.graaljs.getnodes.cmp")
public class LiteFlowScriptGetNodesGraaljsTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void getNodesTest1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		List<Node> nodes = FlowBus.getNodeByChainId("chain1");
		Assertions.assertEquals(5, nodes.size());
	}

	@Test
	public void getNodesTest2() {
		List<Node> nodes = FlowBus.getNodeByChainId("chain2");
		Assertions.assertEquals(5, nodes.size());
	}

}
