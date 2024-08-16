package com.yomahub.liteflow.test.sql.cmp;

import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.spi.holder.SpiFactoryInitializing;
import com.yomahub.liteflow.spring.ComponentScanner;
import com.yomahub.liteflow.thread.ExecutorHelper;
import org.junit.jupiter.api.Test;

public class refresh {

    @Test
    public void cleanScanCache() {
        ComponentScanner.cleanCache();
        FlowBus.cleanCache();
        ExecutorHelper.loadInstance().clearExecutorServiceMap();
        SpiFactoryInitializing.clean();
        LiteflowConfigGetter.clean();
        FlowInitHook.cleanHook();
    }
}
