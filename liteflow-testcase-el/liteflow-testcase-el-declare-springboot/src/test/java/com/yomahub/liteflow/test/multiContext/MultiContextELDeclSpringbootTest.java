package com.yomahub.liteflow.test.multiContext;

import cn.hutool.core.date.DateUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.NoSuchContextBeanException;
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
 * springboot环境最普通的例子测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/multiContext/application.properties")
@SpringBootTest(classes = MultiContextELDeclSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.multiContext.cmp" })
public class MultiContextELDeclSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testMultiContext1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", OrderContext.class, CheckContext.class);
		OrderContext orderContext = response.getContextBean(OrderContext.class);
		CheckContext checkContext = response.getContextBean(CheckContext.class);
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("987XYZ", checkContext.getSign());
		Assertions.assertEquals(95, checkContext.getRandomId());
		Assertions.assertEquals("SO12345", orderContext.getOrderNo());
		Assertions.assertEquals(2, orderContext.getOrderType());
		Assertions.assertEquals(DateUtil.parseDate("2022-06-15"), orderContext.getCreateTime());
	}

	@Test
	public void testMultiContext2() throws Exception {
		Assertions.assertThrows(NoSuchContextBeanException.class, () -> {
			LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", OrderContext.class, CheckContext.class);
			DefaultContext context = response.getContextBean(DefaultContext.class);
		});
	}

}
