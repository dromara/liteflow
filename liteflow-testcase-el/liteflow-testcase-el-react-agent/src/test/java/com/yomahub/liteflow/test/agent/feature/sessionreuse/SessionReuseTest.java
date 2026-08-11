package com.yomahub.liteflow.test.agent.feature.sessionreuse;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * 覆盖 guide §5.1：同一 {@code (conversationId, agentKey)} 的多次调用复用同一 ReActAgent。
 *
 * <p>通过探针抓取的 agentId 判断：两次执行 agentId 一致即说明组件持有的 Agent
 * runtime 被复用。
 */
@TestPropertySource("classpath:/feature/sessionreuse/application.properties")
@SpringBootTest(classes = SessionReuseTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.sessionreuse")
public class SessionReuseTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        MemoryAgentCmp.reset();
        MemoryAgentCmp.resetModelObservations();
    }

    @Test
    public void testSameConversationReusesAgentInstance() {
        LiteflowResponse first = flowExecutor.execute2Resp("memoryChain", "你是谁？");
        Assertions.assertTrue(first.isSuccess(),
                "first chain failed: " + (first.getCause() == null ? "" : first.getCause().getMessage()));
        String firstAgentId = MemoryAgentCmp.PROBE.get().observedAgentId();
        Assertions.assertNotNull(firstAgentId);

        MemoryAgentCmp.reset();
        LiteflowResponse second = flowExecutor.execute2Resp("memoryChain", "请重复一下上一句话。");
        Assertions.assertTrue(second.isSuccess(),
                "second chain failed: " + (second.getCause() == null ? "" : second.getCause().getMessage()));
        String secondAgentId = MemoryAgentCmp.PROBE.get().observedAgentId();
        Assertions.assertEquals(firstAgentId, secondAgentId,
                "同一 (conversationId, agentKey) 多次调用应复用同一个 ReActAgent 实例");
        Assertions.assertTrue(MemoryAgentCmp.PROBE.get().reasoningCount() > 0,
                "runtime middleware must forward callbacks to the replacement probe");
        Assertions.assertEquals(2, MemoryAgentCmp.modelMessageCounts().size());
        Assertions.assertTrue(
                MemoryAgentCmp.modelMessageCounts().get(1)
                        > MemoryAgentCmp.modelMessageCounts().get(0),
                "second model invocation must receive the persisted first-turn history");
    }
}
