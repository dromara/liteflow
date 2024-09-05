package com.yomahub.liteflow.test.processFact;

import cn.hutool.core.date.DateUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.processFact.context.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * process方法上的fact映射参数测试
 * @author Bryan.Zhang
 */
@Import(profiles ="classpath:/processFact/application.properties")
@SolonTest
public class ProcessFactELDeclMultiSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	// 最基本的情况
	@Test
	public void testFact1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", createContext());
		Assertions.assertTrue(response.isSuccess());
		TestContext context = response.getContextBean(TestContext.class);
		Assertions.assertEquals("jack", context.getUser().getName());
	}

	// 多上下文自动搜寻
	@Test
	public void testFact2() throws Exception {
		TestContext testContext = createContext();
		Demo1Context demo1Context = new Demo1Context();
		demo1Context.setData1("xxxx");
		demo1Context.setData2(99);

		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg", demo1Context, testContext);
		Assertions.assertTrue(response.isSuccess());
		TestContext context = response.getContextBean(TestContext.class);
		Assertions.assertEquals(20, context.getUser().getCompany().getHeadCount());
	}

	// 多上下文都有user，指定上下文中的user
	@Test
	public void testFact3() throws Exception {
		TestContext testContext = createContext();
		Demo2Context demo2Context = createDemo2Context();

		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg", testContext, demo2Context);
		Assertions.assertTrue(response.isSuccess());
		Demo2Context context = response.getContextBean(Demo2Context.class);
		Assertions.assertEquals("rose", context.getUser().getName());
	}

	// 多上下文都有user，指定上下文中的user
	@Test
	public void testFact4() throws Exception {
		Demo3Context demo3Context = createDemo3Context();

		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg", demo3Context);
		Assertions.assertTrue(response.isSuccess());
		Demo3Context context = response.getContextBean(Demo3Context.class);
		Assertions.assertEquals("jelly", context.getUser().getName());
	}

	private TestContext createContext(){
		Company company = new Company("XXX有限公司", "黄河路34号303室", 400);
		User user = new User("张三", 18, DateUtil.parseDate("1990-08-20"), company);
        return new TestContext(user, "this is data");
	}

	private Demo2Context createDemo2Context(){
		Company company = new Company("XXX有限公司", "和平路12号101室", 600);
		User user = new User("李四", 28, DateUtil.parseDate("1990-06-01"), company);
		return new Demo2Context("xxx", user);
	}

	private Demo3Context createDemo3Context(){
		Company company = new Company("XXX有限公司", "和平路12号101室", 600);
		User user = new User("王五", 28, DateUtil.parseDate("1990-06-01"), company);
		return new Demo3Context("xxx", user);
	}

}
