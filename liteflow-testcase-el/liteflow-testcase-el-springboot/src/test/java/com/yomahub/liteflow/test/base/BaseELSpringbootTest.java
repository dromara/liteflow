package com.yomahub.liteflow.test.base;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
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
 * @author luo yi
 */
@TestPropertySource(value = "classpath:/base/application.properties")
@SpringBootTest(classes = BaseELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.base.cmp" })
public class BaseELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 最简单的情况
	@Test
	public void testBase1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// switch节点最简单的测试用例
	@Test
	public void testBase2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// then,when,switch混用的稍微复杂点的用例,switch跳到一个then上
	@Test
	public void testBase3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// 一个非常复杂的例子，可以看base目录下的img.png这个图示
	@Test
	public void testBase4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// 用变量来声明短流程
	@Test
	public void testBase5() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// 入参执行 EL 表达式
	@Test
	public void testBase6() throws Exception {
		LiteflowResponse response = flowExecutor.execute2RespWithEL("THEN(a, b,c);;");
		Assertions.assertTrue(response.isSuccess());

		LiteflowResponse response1 = flowExecutor.execute2RespWithEL("THEN(\na, \tb,c);");
		Assertions.assertTrue(response1.isSuccess());

		Assertions.assertEquals(response.getChainId(), response1.getChainId());
	}

	// 入参执行 EL 表达式，测试移除 chain
	@Test
	public void testBase7() throws Exception {
		LiteflowResponse response = flowExecutor.execute2RespWithEL("THEN(a,b, \nc);;");
		Assertions.assertTrue(response.isSuccess());

		FlowBus.removeChain(response.getChainId());

		LiteflowResponse response1 = flowExecutor.execute2RespWithEL("THEN(a,b, c);");
		Assertions.assertTrue(response1.isSuccess());

		Assertions.assertNotEquals(response.getChainId(), response1.getChainId());
	}

	// 运行文件里同样的 chain
	@Test
	public void testBase8() throws Exception {
		LiteflowResponse response = flowExecutor.execute2RespWithEL("THEN(a,b,SWITCH(e).to(d,f));");
		Assertions.assertTrue(response.isSuccess());
		// 应返回 chain2
		Assertions.assertEquals("chain2", response.getChainId());

		LiteflowResponse response1 = flowExecutor.execute2RespWithEL("t1 = THEN(c, WHEN(j,k));\n" +
                "        w1 = WHEN(q, THEN(p, r)).id(\"w01\");\n" +
                "        t2 = THEN(h, i);\n" +
                "\n" +
                "        THEN(\n" +
                "        a,b,\n" +
                "        WHEN(t1, d, t2 ),\n" +
                "        SWITCH(x).to(m, n, w1),\n" +
                "        z\n" +
                "        );\n" +
                "        THEN(a,b,b,a,SWITCH(e).TO(d,b));");
		Assertions.assertTrue(response1.isSuccess());
		// 应返回 chain5
		Assertions.assertEquals("chain5", response1.getChainId());
	}

}
