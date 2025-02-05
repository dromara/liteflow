package com.yomahub.liteflow.test.getnodes;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.List;

/**
 * springboot环境LiteflowMetaOperator.getNodes的例子测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/getnodes/application.properties")
@SpringBootTest(classes = GetNodesELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.getnodes.cmp" })
public class GetNodesELSpringbootTest extends BaseTest {

	// 从简单到复杂
	@Test
	public void testGetNodes1() throws Exception {
		List<Node> nodeList1 = LiteflowMetaOperator.getNodes("chain1");
		Assertions.assertEquals(4, nodeList1.size());
		List<Node> nodeList2 = LiteflowMetaOperator.getNodes("chain2");
		Assertions.assertEquals(5, nodeList2.size());
		List<Node> nodeList3 = LiteflowMetaOperator.getNodes("chain3");
		Assertions.assertEquals(7, nodeList3.size());
		List<Node> nodeList4 = LiteflowMetaOperator.getNodes("chain4");
		Assertions.assertEquals(15, nodeList4.size());
	}

	// 测试有子变量的情况
	@Test
	public void testGetNodes2() throws Exception {
		List<Node> nodeList = LiteflowMetaOperator.getNodes("chain5");
		Assertions.assertEquals(15, nodeList.size());
	}

	// 测试有子chain的情况
	@Test
	public void testGetNodes3() throws Exception {
		List<Node> nodeList = LiteflowMetaOperator.getNodes("chain6");
		Assertions.assertEquals(6, nodeList.size());
	}
}
