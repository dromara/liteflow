package com.yomahub.liteflow.test.privateDelivery;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * springboot环境下隐私投递的测试
 *
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/privateDelivery/application.properties")
@SpringBootTest(classes = PrivateDeliveryELDeclSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.privateDelivery.cmp" })
public class PrivateDeliveryELDeclSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testPrivateDelivery() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		ConcurrentHashSet<Integer> set = context.getData("testSet");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals(100, set.size());
	}

}
