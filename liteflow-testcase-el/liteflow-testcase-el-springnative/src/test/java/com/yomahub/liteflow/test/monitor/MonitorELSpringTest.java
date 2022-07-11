package com.yomahub.liteflow.test.monitor;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.monitor.MonitorBus;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * spring环境下监控的测试
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/monitor/application.xml")
public class MonitorELSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testMonitor() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());

        Thread.sleep(10000);
    }

    @AfterClass
    public static void clean(){
        MonitorBus monitorBus = ContextAwareHolder.loadContextAware().getBean(MonitorBus.class);
        monitorBus.closeScheduler();
    }

}
