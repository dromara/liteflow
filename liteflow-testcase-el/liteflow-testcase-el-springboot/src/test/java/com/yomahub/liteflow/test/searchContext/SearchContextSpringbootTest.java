package com.yomahub.liteflow.test.searchContext;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.searchContext.context.Member;
import com.yomahub.liteflow.test.searchContext.context.MemberContext;
import com.yomahub.liteflow.test.searchContext.context.OrderContext;
import com.yomahub.liteflow.test.searchContext.context.UserInfoContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/searchContext/application.properties")
@SpringBootTest(classes = SearchContextSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.searchContext.cmp"})
public class SearchContextSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试动态取，动态设置的基础情况
	@Test
	public void testSearchContext1() throws Exception {
		MemberContext memberContext = new MemberContext();
		memberContext.setId(31);
		memberContext.setName("jack");
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", memberContext, new DefaultContext());
		Assertions.assertTrue(response.isSuccess());
		MemberContext context = response.getContextBean(MemberContext.class);
		Assertions.assertEquals("hello,jack", context.getName());
	}

	// 测试动态取，2个上下文智能设置，1个参数
	@Test
	public void testSearchContext2() throws Exception {
		MemberContext memberContext = new MemberContext();
		memberContext.setId(31);
		memberContext.setName("jack");
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg", memberContext, new DefaultContext());
		Assertions.assertTrue(response.isSuccess());
		MemberContext context = response.getContextBean(MemberContext.class);
		Assertions.assertEquals("hello,jack", context.getName());
	}

	// 多级动态取，2个上下文智能设置，多个参数
	@Test
	public void testSearchContext3() throws Exception {
		OrderContext orderContext = new OrderContext();
		orderContext.setOrderCode("SO1234");
		orderContext.setMember(new Member("M0001","jack"));
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg", orderContext, new DefaultContext());
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getContextBean(DefaultContext.class);
		Assertions.assertEquals("M0001", context.getData("test"));
	}

	// 3个上下文指定上下文取和设置
	@Test
	public void testSearchContext4() throws Exception {
		MemberContext memberContext = new MemberContext();
		memberContext.setId(2000);
		memberContext.setName("jack");

		OrderContext orderContext = new OrderContext();
		orderContext.setId(3000);
		orderContext.setOrderCode("SO1234");
		orderContext.setMember(new Member("M0001","jack"));

		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg", memberContext, orderContext, new DefaultContext());
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getContextBean(DefaultContext.class);
		Assertions.assertEquals(3000, (Integer) context.getData("test"));
	}

	// 多个上下文，结合@ContextBean测试
	@Test
	public void testSearchContext5() throws Exception {
		UserInfoContext userInfoContext = new UserInfoContext();
		userInfoContext.setInfo("test info");

		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg", userInfoContext, new DefaultContext());
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getContextBean(DefaultContext.class);
		Assertions.assertEquals("test info", context.getData("test"));
	}

	// 测试获取DefaultContext
	@Test
	public void testSearchContext6() throws Exception {
		DefaultContext context = new DefaultContext();
		context.setData("k1", "v1");

		LiteflowResponse response = flowExecutor.execute2Resp("chain6", "arg", context);
		Assertions.assertTrue(response.isSuccess());
		context = response.getContextBean(DefaultContext.class);
		Assertions.assertEquals("v1", context.getData("test"));
	}

}
