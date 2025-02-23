package com.yomahub.liteflow.test.bindData;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ObjectConvertException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.bindData.context.Member;
import com.yomahub.liteflow.test.bindData.context.MemberContext;
import com.yomahub.liteflow.test.bindData.context.OrderContext;
import com.yomahub.liteflow.test.bindData.context.UserInfoContext;
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
@TestPropertySource(value = "classpath:/bindData/application2.properties")
@SpringBootTest(classes = BindDataSpringbootTest2.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.bindData.cmp2"})
public class BindDataSpringbootTest2 extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试动态bind，最简单的情况，2个上下文中搜索
	@Test
	public void testBindDynamic1() throws Exception {
		MemberContext memberContext = new MemberContext();
		memberContext.setId(31);
		memberContext.setName("jack");
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", memberContext, new DefaultContext());
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getContextBean(DefaultContext.class);
		Assertions.assertEquals("jack", context.getData("a1"));
	}

	// 测试动态bind，多级取数据，2个上下文中搜索
	@Test
	public void testBindDynamic2() throws Exception {
		OrderContext orderContext = new OrderContext();
		orderContext.setOrderCode("SO1234");
		orderContext.setMember(new Member("M0001","jack"));
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg", orderContext, new DefaultContext());
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getContextBean(DefaultContext.class);
		Assertions.assertEquals("M0001", context.getData("a2"));
	}

	// 测试动态bind，多个上下文，拥有相同的变量，指定上下文
	@Test
	public void testBindDynamic3() throws Exception {
		OrderContext orderContext = new OrderContext();
		orderContext.setId(1000);
		orderContext.setOrderCode("SO1234");
		orderContext.setMember(new Member("M0001","jack"));

		MemberContext memberContext = new MemberContext();
		memberContext.setId(2000);
		memberContext.setName("jack");

		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg", orderContext, memberContext, new DefaultContext());
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getContextBean(DefaultContext.class);
		Assertions.assertEquals(2000, (Integer) context.getData("a3"));
	}

	// 测试动态bind，多个上下文，结合@ContextBean测试
	@Test
	public void testBindDynamic4() throws Exception {
		UserInfoContext userInfoContext = new UserInfoContext();
		userInfoContext.setInfo("test info");

		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg", userInfoContext, new DefaultContext());
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getContextBean(DefaultContext.class);
		Assertions.assertEquals("test info", context.getData("a4"));
	}

	// 测试动态bind，getBindData中的class给错，报错
	@Test
	public void testBindDynamic5() throws Exception {
		MemberContext memberContext = new MemberContext();
		memberContext.setId(31);
		memberContext.setName("jack");
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg", memberContext, new DefaultContext());
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals(ObjectConvertException.class, response.getCause().getClass());
	}

}
