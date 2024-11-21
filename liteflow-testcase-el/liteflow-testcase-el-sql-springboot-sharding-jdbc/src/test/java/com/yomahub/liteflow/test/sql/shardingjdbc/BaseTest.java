package com.yomahub.liteflow.test.sql.shardingjdbc;


import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.spi.holder.SpiFactoryInitializing;
import com.yomahub.liteflow.spring.ComponentScanner;
import com.yomahub.liteflow.thread.ExecutorHelper;
import org.junit.jupiter.api.AfterAll;

/**
 * @author tangkc
 * @since 2.8.6
 */
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
    }

}
