package com.yomahub.liteflow.test.resizeSlot;

import cn.hutool.core.util.ReflectUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * springboot环境下slot扩容测试
 *
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/resizeSlot/application.xml")
public class ResizeSlotELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testResize() throws Exception {
		ExecutorService pool = Executors.newCachedThreadPool();

		List<Future<LiteflowResponse>> futureList = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			Future<LiteflowResponse> future = pool.submit(() -> flowExecutor.execute2Resp("chain1", "arg"));
			futureList.add(future);
		}

		for (Future<LiteflowResponse> future : futureList) {
			Assertions.assertTrue(future.get().isSuccess());
		}

		// 取到static的对象QUEUE
		Field field = ReflectUtil.getField(DataBus.class, "QUEUE");
		ConcurrentLinkedQueue<Integer> queue = (ConcurrentLinkedQueue<Integer>) ReflectUtil.getStaticFieldValue(field);

		// 因为初始slotSize是4，按照0.75的扩容比，要满足100个线程，应该扩容5~6次，5次=65，6次=114
		// 为什么不是直接114呢？
		// 因为在单测中根据机器的性能，在多线程情况下，有些机器跑的慢一点，也就是说65个就足够了。有些机器跑的快一点，是能真正扩容到114个的
		Assertions.assertTrue(queue.size() > 4);
	}

}
