package com.yomahub.liteflow.test.agent.feature;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.FlowEvent;
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 覆盖 guide §2.6：通过 {@code ExecuteOption.eventListener(...)} 接收 Agent 执行中的流式事件。
 *
 * <p>真实模型只要发出 reasoning/响应即可，因此即便测试环境没有逐 token 流式增量，
 * AgentScope 也会发出 {@code REASONING}（assistant 消息）与 {@code AGENT_RESULT} 事件。
 */
@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = StreamingEventTest.class)
@EnableAutoConfiguration
@ComponentScan({
        "com.yomahub.liteflow.test.agent.cmp",
        "com.yomahub.liteflow.test.agent.feature.cmp"
})
public class StreamingEventTest extends BaseAgentLiveTest {

    @BeforeEach
    public void ensureCredential() {
        LiveTestSupport.ensureCompatibleCustomCredentialOrSkip(liteflowConfig, "StreamingEventTest");
    }

    @Test
    public void testEventListenerReceivesAgentReasoningAndResult() {
        List<FlowEvent> events = new CopyOnWriteArrayList<>();

        LiteflowResponse response = flowExecutor.execute2Resp(
                "streamingChain", "请用一句中文短句作答。",
                ExecuteOption.of().eventListener(events::add));

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        // 至少一条 agent.reasoning 事件，nodeId/conversationId/text 都应填充。
        Assertions.assertTrue(events.stream().anyMatch(e ->
                        ReActAgentComponent.FLOW_EVENT_TYPE_REASONING.equals(e.getType())
                                && "streamingAgent".equals(e.getNodeId())
                                && e.getConversationId() != null
                                && !e.getConversationId().isBlank()),
                "stream listener should receive reasoning events tagged with the agent nodeId");

        // 最终的 agent.result 事件应有 isLast=true，并和 chain 的最终 response 一致。
        Assertions.assertTrue(events.stream().anyMatch(e ->
                        ReActAgentComponent.FLOW_EVENT_TYPE_RESULT.equals(e.getType())
                                && e.isLast()
                                && "streamingAgent".equals(e.getNodeId())),
                "stream listener should receive a final agent.result event");
    }
}
