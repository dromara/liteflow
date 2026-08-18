package com.yomahub.liteflow.test.metrics;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.metrics.ChainMetricsLifeCycle;
import com.yomahub.liteflow.metrics.LiteflowMetaView;
import com.yomahub.liteflow.metrics.LiteflowMeterBinder;
import com.yomahub.liteflow.metrics.NodeMetricsLifeCycle;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

/**
 * 验证liteflow.enable=false时，指标自动装配同步退出。
 *
 * 背景：LiteflowConfig的装配不受liteflow.enable控制，而指标自动装配只判断
 * {@code liteflow.metrics.enabled}。当classpath存在actuator且liteflow.enable=false时，
 * LiteflowMeterBinder仍会被装配，其bindTo触发FlowBus静态块，
 * 进而走到LiteflowConfigGetter.get()，而此时SpringAware尚未注入applicationContext，
 * 启动直接NPE。
 *
 * 注意：本用例不继承BaseTest，因为BaseTest的清理钩子会触碰FlowBus，
 * 在liteflow核心被关闭的场景下，FlowBus静态块本身就会因SpringAware未就绪而初始化失败。
 */
@TestPropertySource(properties = "liteflow.enable=false")
@SpringBootTest(classes = LiteflowDisabledSpringbootTest.class)
@EnableAutoConfiguration
public class LiteflowDisabledSpringbootTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testMetricsBeansNotAssembledWhenLiteflowDisabled() {
        // liteflow核心被关闭
        Assertions.assertEquals(0, applicationContext.getBeanNamesForType(FlowExecutor.class).length);
        // LiteflowConfig不受enable开关控制，仍被装配（这正是缺陷触发的前提）
        Assertions.assertEquals(1, applicationContext.getBeanNamesForType(LiteflowConfig.class).length);
        // 指标相关的bean不应再装配
        Assertions.assertEquals(0, applicationContext.getBeanNamesForType(LiteflowMeterBinder.class).length);
        Assertions.assertEquals(0, applicationContext.getBeanNamesForType(ChainMetricsLifeCycle.class).length);
        Assertions.assertEquals(0, applicationContext.getBeanNamesForType(NodeMetricsLifeCycle.class).length);
        Assertions.assertEquals(0, applicationContext.getBeanNamesForType(LiteflowMetaView.class).length);
    }

}
