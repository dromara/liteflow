package com.yomahub.liteflow.test.iterator;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.collection.ListUtil;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/iterator/application.properties")
@SpringBootTest(classes = IteratorELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.iterator.cmp" })
public class IteratorELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 最简单的情况
	@Test
	public void testIt1() throws Exception {
		List<String> list = ListUtil.toList("1", "2", "3");
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", list);
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		String str = context.getData("test");
		Assertions.assertEquals("123", str);
	}

	// 迭代器带break
	@Test
	public void testIt2() throws Exception {
		List<String> list = ListUtil.toList("1", "2", "3");
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", list);
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		String str = context.getData("test");
		Assertions.assertEquals("12", str);
	}

	// 多层迭代
	@Test
	public void testIt3() throws Exception {
		DefaultContext context = new DefaultContext();
		context.setData("set", new HashSet<>());
		context.setData("list1", ListUtil.toList("a", "b", "c"));
		context.setData("list2", ListUtil.toList("1", "2", "3", "4"));
		LiteflowResponse response = flowExecutor.execute2Resp("chain3",null, context);
		Assertions.assertTrue(response.isSuccess());
	}

	//多层迭代循环，取各层obj
	@Test
	public void testIt4() throws Exception {
		DefaultContext context = new DefaultContext();
		context.setData("set", new HashSet<>());
		context.setData("list1", ListUtil.toList("a", "b", "c"));
		context.setData("list2", ListUtil.toList("11", "22"));

		LiteflowResponse response = flowExecutor.execute2Resp("chain4", null, context);
		String indexStr = context.getData("index_str");
		String objStr = context.getData("obj_str");
		Assertions.assertEquals("[00][01][10][11][20][21]", indexStr);
		Assertions.assertEquals("[a11][a22][b11][b22][c11][c22]", objStr);
		Assertions.assertTrue(response.isSuccess());
	}

	//测试多层迭代异步循环的正确性
	@Test
	public void testIt5() throws Exception {
		DefaultContext context = new DefaultContext();
		context.setData("set", new ConcurrentHashSet<>());
		context.setData("list1", ListUtil.toList("a", "b", "c", "d", "e", "f"));
		context.setData("list2", ListUtil.toList("L2_1", "L2_2", "L2_3"));
		context.setData("list3", ListUtil.toList("L3_1", "L3_2", "L3_3", "L3_4"));
		context.setData("list4", ListUtil.toList("L4_1", "L4_2"));
		context.setData("list5", ListUtil.toList("L5_1", "L5_2", "L5_3"));
		LiteflowResponse response = flowExecutor.execute2Resp("chain5",null, context);
		Assertions.assertTrue(response.isSuccess());
		Set<String> set = context.getData("set");
		Assertions.assertEquals(6*3*4*2*3, set.size());
	}
}
