package com.yomahub.liteflow.test;

import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.spi.holder.SpiFactoryCleaner;
import com.yomahub.liteflow.thread.ExecutorHelper;
import org.junit.AfterClass;

public class BaseTest {

    @AfterClass
    public static void cleanScanCache(){
        FlowBus.cleanCache();
        ExecutorHelper.loadInstance().clearExecutorServiceMap();
        SpiFactoryCleaner.clean();
        LiteflowConfigGetter.clean();
        FlowInitHook.cleanHook();
    }
}
