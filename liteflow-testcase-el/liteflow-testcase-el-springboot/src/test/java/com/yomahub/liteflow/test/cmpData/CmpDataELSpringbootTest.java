package com.yomahub.liteflow.test.cmpData;

import cn.hutool.core.date.DateUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.cmpData.vo.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/cmpData/application.properties")
@SpringBootTest(classes = CmpDataELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.cmpData.cmp" })
public class CmpDataELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 最简单的情况
	@Test
	public void testCmpData1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		User user = context.getData("user");
		Assertions.assertEquals(27, user.getAge());
		Assertions.assertEquals("jack", user.getName());
		Assertions.assertEquals(0, user.getBirth().compareTo(DateUtil.parseDate("1995-10-01").toJdkDate()));
	}

	@Test
	public void testCmpData2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg", TestContext.class);
		Assertions.assertTrue(response.isSuccess());
		TestContext context = response.getFirstContextBean();
		Assertions.assertEquals(8, context.getSet().size());
		String result = context.getSet().stream().sorted().collect(Collectors.joining());
		Assertions.assertEquals("12345678", result);
	}

	@Test
	public void testCmpDataList() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		List<User> users = context.getData("users");
		Assertions.assertEquals(3, users.size());
        Assertions.assertNotNull(users.get(0));
		Assertions.assertEquals("jack", users.get(0).getName());
		Assertions.assertEquals(27, users.get(0).getAge());
		Assertions.assertEquals(0, users.get(0).getBirth().compareTo(DateUtil.parseDate("1995-10-01").toJdkDate()));

		Assertions.assertNotNull(users.get(1));
		Assertions.assertEquals("mike", users.get(1).getName());
		Assertions.assertEquals(32, users.get(1).getAge());
		Assertions.assertEquals(0, users.get(1).getBirth().compareTo(DateUtil.parseDate("1992-08-16").toJdkDate()));

		Assertions.assertNotNull(users.get(2));
		Assertions.assertEquals("david", users.get(2).getName());
		Assertions.assertEquals(11, users.get(2).getAge());
		Assertions.assertEquals(0, users.get(2).getBirth().compareTo(DateUtil.parseDate("2013-09-27").toJdkDate()));

		List<User> empty = context.getData("empty");
        Assertions.assertEquals(0, empty.size());
	}

}
