package com.yomahub.liteflow.test;

import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.spi.holder.SpiFactoryCleaner;
import com.yomahub.liteflow.thread.ExecutorHelper;
import org.junit.jupiter.api.AfterAll;

public class BaseTest {

	@AfterAll
	public static void cleanScanCache() {
		FlowBus.cleanCache();
		ExecutorHelper.loadInstance().clearExecutorServiceMap();
		SpiFactoryCleaner.clean();
		LiteflowConfigGetter.clean();
		FlowExecutorHolder.clean();
		FlowInitHook.cleanHook();
		FlowBus.clearStat();
	}

}
