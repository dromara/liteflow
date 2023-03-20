package com.yomahub.liteflow.test.subflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 测试显示调用子流程(yml) 单元测试
 *
 * @author justin.xu
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/subflow/application-yml.properties")
@SpringBootTest(classes = SubflowYmlELDeclMultiSpringBootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.subflow.cmp1" })
public class SubflowYmlELDeclMultiSpringBootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 是否按照流程定义配置执行
	@Test
	public void testExplicitSubFlowYml() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a request");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("a==>b==>c==>b==>a==>e==>d", response.getExecuteStepStr());
	}

}
