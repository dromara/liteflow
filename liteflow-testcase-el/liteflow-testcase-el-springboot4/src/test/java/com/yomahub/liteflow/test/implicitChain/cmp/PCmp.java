package com.yomahub.liteflow.test.implicitChain.cmp;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.Slot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component("p")
public class PCmp extends NodeComponent {

	private ExecutorService executorService = TtlExecutors.getTtlExecutorService(Executors.newCachedThreadPool());

	@Override
	public void process() throws Exception {
		List<CompletableFuture<Void>> fList = new ArrayList<>();
		Slot slot = this.getSlot();
		for (int i = 0; i < 10; i++) {
			int finalI = i;

			CompletableFuture<Void> future = CompletableFuture.runAsync(
					() -> invoke2Resp("c2", "it's implicit subflow " + finalI, slot), executorService
			);

			fList.add(future);
		}

		CompletableFuture.allOf(fList.toArray(new CompletableFuture[0])).get();

	}

}
