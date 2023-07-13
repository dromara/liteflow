package com.yomahub.liteflow.test.slotOffer;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@SpringBootTest(classes = SlotOfferELSpringbootTest.class)
@EnableAutoConfiguration
public class SlotOfferELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 并发200，一共6w条线程去获取Slot的测试
	@Test
	public void testSlotOffer() throws Exception {
		Set<Integer> set = new ConcurrentHashSet<>();
		Set<CompletableFuture<Boolean>> futureSet = new ConcurrentHashSet<>();
		Set<String> error = new ConcurrentHashSet<>();
		for (int i = 0; i < 60000; i++) {
			futureSet.add(CompletableFuture.supplyAsync(() -> {
				int index = 0;
				try {
					index = DataBus.offerSlotByClass(ListUtil.toList(DefaultContext.class));
					boolean flag = set.add(index);
					if (!flag) {
						error.add(Integer.toString(index));
					}
				}
				catch (Exception e) {
					error.add(e.getMessage());
				}
				finally {
					DataBus.releaseSlot(index);
					boolean flag = set.remove(index);
					if (!flag) {
						error.add(Integer.toString(index));
					}
				}
				return Boolean.TRUE;
			}));
		}
		CompletableFuture<Void> resultFuture = CompletableFuture.allOf(futureSet.toArray(new CompletableFuture[] {}));

		resultFuture.get();

		Assertions.assertEquals(0, set.size());
		Assertions.assertEquals(0, error.size());
		Assertions.assertEquals(0, DataBus.OCCUPY_COUNT.get());
	}

}
