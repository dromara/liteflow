package com.yomahub.liteflow.test.aop;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.aop.aspect.CustomAspect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

/**
 * 切面场景单元测试 在声明式组件场景中，自定义aspect的aop不生效的，因为生成的代理类并不是原类，也不是原类的子类，而是NodeComponent的子类
 * 所以切不到，暂且没有想出办法来解决，这个测试类暂时不用
 *
 * @author Bryan.Zhang
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/aop/application.properties")
@SpringBootTest(classes = CustomAOPELDeclSpringbootTest.class)
@EnableAutoConfiguration
@Import(CustomAspect.class)
@ComponentScan({"com.yomahub.liteflow.test.aop.cmp1", "com.yomahub.liteflow.test.aop.cmp2"})

public class CustomAOPELDeclSpringbootTest extends BaseTest {


	@Resource
	private FlowExecutor flowExecutor;

	//测试自定义AOP，串行场景
	@Test
	public void testCustomAopS() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a request");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("before_after", context.getData("a"));
		Assertions.assertEquals("before_after", context.getData("b"));
		Assertions.assertEquals("before_after", context.getData("c"));
	}

	//测试自定义AOP，并行场景
	@Test
	public void testCustomAopP() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "it's a request");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("before_after", context.getData("a"));
		Assertions.assertEquals("before_after", context.getData("b"));
		Assertions.assertEquals("before_after", context.getData("c"));
	}
}