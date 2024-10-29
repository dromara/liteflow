package com.yomahub.liteflow.test.script;

import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.lifecycle.LifeCycleHolder;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.spi.holder.SpiFactoryInitializing;
import com.yomahub.liteflow.spring.ComponentScanner;
import com.yomahub.liteflow.thread.ExecutorHelper;
import org.junit.jupiter.api.AfterAll;

public class BaseTest {

	@AfterAll
	public static void cleanScanCache() {
		ComponentScanner.cleanCache();
		FlowBus.cleanCache();
		ExecutorHelper.loadInstance().clearExecutorServiceMap();
		SpiFactoryInitializing.clean();
		LiteflowConfigGetter.clean();
		FlowInitHook.cleanHook();
		FlowBus.clearStat();
		LifeCycleHolder.clean();
	}

}
