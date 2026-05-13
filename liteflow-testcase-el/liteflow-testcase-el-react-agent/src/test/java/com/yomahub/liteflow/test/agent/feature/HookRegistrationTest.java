package com.yomahub.liteflow.test.agent.feature;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.feature.cmp.HookAgentCmp;
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
 * 覆盖 guide §3 {@code hooks()} 扩展点：覆写返回的 Hook 必须在 Agent 构建后注册到 ReActAgent，
 * 在真实模型调用过程中能收到 PreReasoning / PreActing 等生命周期事件。
 */
@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = HookRegistrationTest.class)
@EnableAutoConfiguration
@ComponentScan({
        "com.yomahub.liteflow.test.agent.cmp",
        "com.yomahub.liteflow.test.agent.feature.cmp"
})
public class HookRegistrationTest extends BaseAgentLiveTest {

    @BeforeEach
    public void resetProbe() {
        HookAgentCmp.reset();
        LiveTestSupport.ensureCompatibleCustomCredentialOrSkip(liteflowConfig, "HookRegistrationTest");
    }

    @Test
    public void testComponentHooksAreInvokedDuringAgentExecution() {
        LiteflowResponse response = flowExecutor.execute2Resp("hookChain", "你好，请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Assertions.assertTrue(HookAgentCmp.PROBE.get().reasoningCount() > 0,
                "AgentProbe.hook() 在真实模型调用时应至少收到一次 PreReasoning 事件");
        Assertions.assertNotNull(HookAgentCmp.PROBE.get().observedAgentId());
    }
}
