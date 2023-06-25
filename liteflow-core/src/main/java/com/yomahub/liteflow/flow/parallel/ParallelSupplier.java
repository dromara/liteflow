package com.yomahub.liteflow.flow.parallel;

import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import java.util.function.Supplier;

/**
 * 并行异步worker对象，提供给CompletableFuture用
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
public class ParallelSupplier implements Supplier<WhenFutureObj> {

	private static final LFLog LOG = LFLoggerManager.getLogger(ParallelSupplier.class);

	private final Executable executableItem;

	private final String currChainId;

	private final Integer slotIndex;

	public ParallelSupplier(Executable executableItem, String currChainId, Integer slotIndex) {
		this.executableItem = executableItem;
		this.currChainId = currChainId;
		this.slotIndex = slotIndex;
	}

	@Override
	public WhenFutureObj get() {
		try {
			executableItem.setCurrChainId(currChainId);
			executableItem.execute(slotIndex);
			return WhenFutureObj.success(executableItem.getId());
		}
		catch (Exception e) {
			return WhenFutureObj.fail(executableItem.getId(), e);
		}
	}

}
