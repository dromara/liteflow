package com.yomahub.liteflow.test;

import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.spring.ComponentScanner;
import com.yomahub.liteflow.thread.ExecutorHelper;
import org.junit.AfterClass;

public class BaseTest {

    @AfterClass
    public static void cleanScanCache(){
        ComponentScanner.cleanCache();
        FlowBus.cleanCache();
        ExecutorHelper.loadInstance().setExecutorService(null);
    }
}
