package com.yomahub.liteflow.test.agent.feature.maxiterations;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * 验证 guide §3 中 {@code maxIterations()} 覆写值会传递到 ReActAgent。
 *
 * <p>断言通过组件注册的 Hook 读取 {@code ReActAgent.getMaxIters()}：全局默认值与组件覆写值
 * ({@link MaxIterationsAgentCmp#OVERRIDDEN_MAX_ITERS}=7) 不同，确认覆写生效。
 */
@TestPropertySource("classpath:/feature/maxiterations/application.properties")
@SpringBootTest(classes = MaxIterationsTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.maxiterations")
public class MaxIterationsTest extends BaseAgentLiveTest {

    @BeforeEach
    public void resetProbe() {
        MaxIterationsAgentCmp.reset();
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflowConfig, "MaxIterationsTest");
    }

    @Test
    public void testComponentMaxIterationsOverridesGlobalDefault() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "maxIterationsChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Assertions.assertEquals(MaxIterationsAgentCmp.OVERRIDDEN_MAX_ITERS,
                MaxIterationsAgentCmp.PROBE.get().observedMaxIters(),
                "ReActAgent.getMaxIters() should reflect the component-level override");
    }
}
