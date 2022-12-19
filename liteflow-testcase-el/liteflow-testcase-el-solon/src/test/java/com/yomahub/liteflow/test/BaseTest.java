package com.yomahub.liteflow.test;

import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.spi.holder.SpiFactoryCleaner;
import com.yomahub.liteflow.thread.ExecutorHelper;
import org.junit.AfterClass;
import org.noear.solon.Solon;

public class BaseTest {

    @AfterClass
    public static void cleanScanCache(){
        //Solon.context().clear();
        FlowBus.cleanCache();
        ExecutorHelper.loadInstance().clearExecutorServiceMap();
        SpiFactoryCleaner.clean();
        LiteflowConfigGetter.clean();
        FlowInitHook.cleanHook();
    }
}
