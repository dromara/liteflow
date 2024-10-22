package com.yomahub.liteflow.test.chainThreadPool;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.List;

/**
 * springboot环境下chain线程池隔离测试
 */
@TestPropertySource(value = "classpath:/chainThreadPool/application2.properties")
@SpringBootTest(classes = CustomChainThreadPoolELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.chainThreadPool.cmp"})
public class CustomChainThreadPoolELSpringbootTest extends BaseTest {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Resource
	private FlowExecutor flowExecutor;

	/**
	 * 测试chain自定义线程池隔离
	 */
	@Test
	public void testCustomChainThreadPool1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertTrue(context.getData("threadNameFor").toString().startsWith("customer-chain-thead-1"));
		Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-chain-thead-1"));
	}

	/**
	 * 测试when上自定义线程池和chain线程池隔离-优先以when上为准
	 */
	@Test
	public void testCustomChainThreadPool2() {
		LiteflowResponse response1 = flowExecutor.execute2Resp("chain2", "arg");
		DefaultContext context = response1.getFirstContextBean();
		Assertions.assertTrue(response1.isSuccess());
		Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-chain-thead-2"));
	}

	/**
	 * 测试并行FOR循环全局线程池和chain线程池隔离-优先以chain线程池上为准
	 */
	@Test
	public void testCustomChainThreadPool3() {
		LiteflowResponse response1 = flowExecutor.execute2Resp("chain3", "arg");
		DefaultContext context = response1.getFirstContextBean();
		Assertions.assertTrue(response1.isSuccess());
		Assertions.assertTrue(context.getData("threadNameFor").toString().startsWith("customer-chain-thead-1"));
	}

	/**
	 * 测试并行条件循环全局线程池和chain线程池隔离-优先以chain线程池上为准
	 */
	@Test
	public void testCustomChainThreadPool4() {
		LiteflowResponse response1 = flowExecutor.execute2Resp("chain4", "arg");
		DefaultContext context = response1.getFirstContextBean();
		Assertions.assertTrue(response1.isSuccess());
		Assertions.assertTrue(context.getData("threadNameWhile").toString().startsWith("customer-chain-thead-1"));
	}

	/**
	 * 测试并行迭代循环全局线程池和chain线程池隔离-优先以chain线程池上为准
	 */
	@Test
	public void testCustomChainThreadPool5() {
		List<String> list = ListUtil.toList("1", "2", "3", "4", "5");
		LiteflowResponse response1 = flowExecutor.execute2Resp("chain5", list);
		DefaultContext context = response1.getFirstContextBean();
		Assertions.assertTrue(response1.isSuccess());
		Assertions.assertTrue(context.getData("threadNameIterator").toString().startsWith("customer-chain-thead-1"));
	}


}
