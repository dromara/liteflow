package com.yomahub.liteflow.test.lfCmpAnno;

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
 * 测试@LiteflowComponent标注
 *
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/lfCmpAnno/application.properties")
@SpringBootTest(classes = LiteflowComponentELDeclMultiSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.lfCmpAnno.cmp" })
public class LiteflowComponentELDeclMultiSpringbootTest extends BaseTest {

	@Autowired
	private FlowExecutor flowExecutor;

	@Test
	public void testConfig() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a[A组件]==>b[B组件]==>c[C组件]==>b[B组件]==>a[A组件]==>d", response.getExecuteStepStr());
	}

}
