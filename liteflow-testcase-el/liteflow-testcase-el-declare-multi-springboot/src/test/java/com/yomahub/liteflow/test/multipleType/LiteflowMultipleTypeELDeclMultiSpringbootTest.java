package com.yomahub.liteflow.test.multipleType;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 测试springboot下混合格式规则的场景
 *
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/multipleType/application.properties")
@SpringBootTest(classes = LiteflowMultipleTypeELDeclMultiSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.multipleType.cmp" })
public class LiteflowMultipleTypeELDeclMultiSpringbootTest extends BaseTest {

	@Autowired
	private FlowExecutor flowExecutor;

	@Test
	public void testMultipleType() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>c==>b==>a", response.getExecuteStepStr());
		response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());
	}

}
