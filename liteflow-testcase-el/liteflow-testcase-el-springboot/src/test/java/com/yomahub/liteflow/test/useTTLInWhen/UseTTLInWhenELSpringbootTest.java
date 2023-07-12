package com.yomahub.liteflow.test.useTTLInWhen;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * 在when异步节点的情况下去拿ThreadLocal里的测试场景
 *
 * @author Bryan.Zhang
 * @since 2.6.3
 */
@TestPropertySource(value = "classpath:/useTTLInWhen/application.properties")
@SpringBootTest(classes = UseTTLInWhenELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.useTTLInWhen.cmp" })
public class UseTTLInWhenELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testUseTTLInWhen() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertEquals("hello,b", context.getData("b"));
		Assertions.assertEquals("hello,c", context.getData("c"));
		Assertions.assertEquals("hello,d", context.getData("d"));
		Assertions.assertEquals("hello,e", context.getData("e"));
		Assertions.assertEquals("hello,f", context.getData("f"));
	}

}
