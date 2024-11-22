package com.yomahub.liteflow.test.script.qlexpress;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.entity.CmpStep;
import com.yomahub.liteflow.slot.DefaultContext;
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
import java.util.*;
import java.util.stream.Collectors;

import static com.yomahub.liteflow.enums.NodeTypeEnum.SCRIPT;

/**
 * PARSE_ONE_ON_FIRST_EXEC 第一次执行时解析脚本节点
 * 测试springboot下的脚本组件
 *
 * @author jay li
 * @since 2.12.4
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/xml-script/application-parse-first.properties")
@SpringBootTest(classes = LiteflowXmlScriptQLExpressELParseModeTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.qlexpress.cmp" })
public class LiteflowXmlScriptQLExpressELParseModeTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试启动时未加载，执行后加载
	@Test
	public void testScript() {
		Map<String, Node> nodeMap = FlowBus.getNodeMap();

		for (Map.Entry<String, Node> entry : nodeMap.entrySet()) {
			Node node = entry.getValue();
			if (SCRIPT.equals(node.getType())) {
				Assertions.assertFalse(node.isCompiled());
			}
		}

		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals(Integer.valueOf(6), context.getData("s1"));

		// 验证脚本节点是否被重新编译
		Set<String> nodeIds = response.getExecuteStepQueue().stream().map(CmpStep::getNodeId).collect(Collectors.toSet());
		nodeMap = FlowBus.getNodeMap();
		for (Map.Entry<String, Node> entry : nodeMap.entrySet()) {
			Node node = entry.getValue();
			if (SCRIPT.equals(node.getType()) && nodeIds.contains(node.getId())) {
				Assertions.assertTrue(node.isCompiled());
				Assertions.assertNotNull(node.getInstance());
			}
		}
	}

	// 测试普通脚本节点
	@Test
	public void testScript1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals(Integer.valueOf(6), context.getData("s1"));
	}

	// 测试条件脚本节点
	@Test
	public void testScript2() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("d==>s2[条件脚本]==>b", response.getExecuteStepStr());
	}

	@Test
	public void testScript3() throws Exception {
		// 根据配置，加载的应该是flow.xml，执行原来的规则
		LiteflowResponse responseOld = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(responseOld.isSuccess());
		Assertions.assertEquals("d==>s2[条件脚本]==>b", responseOld.getExecuteStepStr());
		// 更改规则，重新加载，更改的规则内容从flow_update.xml里读取，这里只是为了模拟下获取新的内容。不一定是从文件中读取
		String newContent = ResourceUtil.readUtf8Str("classpath: /xml-script/flow_update.el.xml");
		// 进行刷新
		FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_EL_XML, newContent);

		// 重新执行chain2这个链路，结果会变
		LiteflowResponse responseNew = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(responseNew.isSuccess());
		Assertions.assertEquals("d==>s2[条件脚本_改]==>a==>s3[普通脚本_新增]", responseNew.getExecuteStepStr());
	}

}
