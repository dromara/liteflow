package com.yomahub.liteflow.test.superClassDefine;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
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
 * springboot环境声明式父类声明组件方法测试
 *
 * @author Bryan.Zhang
 * @since 2.9.4
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/superClassDefine/application.properties")
@SpringBootTest(classes = SuperClassDefineELDeclSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.superClassDefine.cmp" })
public class SuperClassDefineELDeclSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testSuperClassDefine() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("a==>b==>c==>d", response.getExecuteStepStr());
		Assert.assertTrue(context.getData("isAccess"));
	}

}
