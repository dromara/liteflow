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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * springboot环境EL异步循环测试
 *
 * @author zhhhhy
 * @since 2.11.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/parallelLoop/application_IASW3I.properties")
@SpringBootTest(classes = ParallelLoopCase_IASW3I.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.parallelLoop.cmp" })
public class ParallelLoopCase_IASW3I extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 为了验证https://gitee.com/dromara/liteFlow/issues/IASW3I
	// 为了验证2点
	// 1.异步循环不是串行的 2.不会有重复，并发问题
	@Test
	public void testParallelLoop1() throws Exception {
		List<Integer> list = IntStream.range(0, 10000).boxed().collect(Collectors.toList());
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", list, DefaultContext.class);
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertEquals(10000, context.dataMap.size());

	}




}
