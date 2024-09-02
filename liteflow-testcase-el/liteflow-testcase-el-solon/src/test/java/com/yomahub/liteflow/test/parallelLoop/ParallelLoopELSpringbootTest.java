package com.yomahub.liteflow.test.parallelLoop;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

import java.util.List;
import java.util.regex.Pattern;

/**
 * solon环境EL异步循环测试
 *
 * @author zhhhhy
 * @since 2.11.0
 */
@SolonTest
@Import(profiles="classpath:/parallelLoop/application.properties")
public class ParallelLoopELSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	//测试并行FOR循环，循环次数直接在el中定义
	@Test
	public void testParallelLoop1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	//测试并行FOR循环，循环次数由For组件定义
	@Test
	public void testParallelLoop2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	//测试并行FOR循环中的BREAK组件能够正常发挥作用
	@Test
	public void testParallelLoop3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	//测试并行FOR循环中，主线程是否会正常等待所有并行子项完成后再继续执行
	@Test
	public void testParallelLoop4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	@Test
	//测试并行FOR循环中，某个并行子项抛出异常
	public void testParallelLoop5() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("300", response.getCode());
		Assertions.assertNotNull(response.getCause());
		Assertions.assertTrue(response.getCause() instanceof LiteFlowException);
		Assertions.assertNotNull(response.getSlot());
	}

	//并行的条件循环
	@Test
	public void testParallelLoop6() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain6", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	//并行的迭代循环
	@Test
	public void testParallelLoop7() throws Exception {
		List<String> list = ListUtil.toList("1", "2", "3", "4", "5");
		LiteflowResponse response = flowExecutor.execute2Resp("chain7", list);
		Assertions.assertTrue(response.isSuccess());
	}

	//测试并行FOR循环中的index
	@Test
	public void testParallelLoop8() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain8", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		String regex = "(?!.*(.).*\\1)[0-4]{5}";   //匹配不包含重复数字的0-4的5位数字
		Pattern pattern = Pattern.compile(regex);
		//e1,e2,e3分别并行执行5次，因此单个循环的顺序可以是任意的
		Assertions.assertTrue(pattern.matcher(context.getData("loop_e1")).matches());
		Assertions.assertTrue(pattern.matcher(context.getData("loop_e2")).matches());
		Assertions.assertTrue(pattern.matcher(context.getData("loop_e3")).matches());
	}


	//测试自定义线程池配置是否生效
	@Test
	public void testParallelLoop9() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain9", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-loop-thead"));
	}



}
