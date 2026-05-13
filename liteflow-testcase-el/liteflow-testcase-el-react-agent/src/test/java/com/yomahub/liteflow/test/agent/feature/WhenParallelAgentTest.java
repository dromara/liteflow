package com.yomahub.liteflow.test.agent.feature;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.feature.cmp.ParallelAgentACmp;
import com.yomahub.liteflow.test.agent.feature.cmp.ParallelAgentBCmp;
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
 * 覆盖 guide §8.3：WHEN 并发执行多个 ReAct Agent。两个分支 Agent 显式覆写
 * agentKey() 为含 requestId 的唯一值，确保它们落在不同 Session（不同的锁），
 * 才能真正并发执行而不互相串行。
 */
@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = WhenParallelAgentTest.class)
@EnableAutoConfiguration
@ComponentScan({
        "com.yomahub.liteflow.test.agent.cmp",
        "com.yomahub.liteflow.test.agent.feature.cmp"
})
public class WhenParallelAgentTest extends BaseAgentLiveTest {

    @BeforeEach
    public void resetProbes() {
        ParallelAgentACmp.reset();
        ParallelAgentBCmp.reset();
        LiveTestSupport.ensureCompatibleCustomCredentialOrSkip(liteflowConfig, "WhenParallelAgentTest");
    }

    @Test
    public void testWhenAgentsBothExecuteAndProduceRepliesInSlot() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "whenParallelChain", "请用一句中文短句作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        Assertions.assertNotNull(ParallelAgentACmp.SEEN_REPLY.get(),
                "WHEN 分支 A 应该执行完毕并写入回复");
        Assertions.assertNotNull(ParallelAgentBCmp.SEEN_REPLY.get(),
                "WHEN 分支 B 应该执行完毕并写入回复");
        Assertions.assertNotEquals(ParallelAgentACmp.SEEN_AGENT_KEY.get(),
                ParallelAgentBCmp.SEEN_AGENT_KEY.get(),
                "两个并发 Agent 应该拥有不同 agentKey，以避免共用同一把 Session 锁");

        Assertions.assertNotNull(response.getSlot().getOutput("parallelAgentA"));
        Assertions.assertNotNull(response.getSlot().getOutput("parallelAgentB"));
    }
}
