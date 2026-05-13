package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.FlowEvent;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.StubReActAgentCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 验证 ReAct Agent 可以通过 LiteFlow 的通用事件回调，在 chain 执行中向调用方推送流式事件。
 */
public class ReActAgentStreamingTest extends AbstractReActAgentSpringbootTest {

    @Test
    public void testAgentStreamEventsDeliveredThroughExecuteOption() {
        List<FlowEvent> events = new CopyOnWriteArrayList<>();

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "stream-me",
                ExecuteOption.of().eventListener(events::add));

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(1, StubReActAgentCmp.HANDLE_REPLY_COUNT.get());
        Assertions.assertNotNull(response.getSlot().getResponseData());

        Assertions.assertTrue(events.stream().anyMatch(event ->
                        "agent.reasoning".equals(event.getType())
                                && "stubAgent".equals(event.getNodeId())
                                && StubReActAgentCmp.FIXED_CONVERSATION_ID.equals(event.getConversationId())
                                && event.getText() != null
                                && event.getText().contains("reply:" + StubReActAgentCmp.FIXED_CONVERSATION_ID)),
                "stream listener should receive reasoning text from the agent while the chain is running");

        Assertions.assertTrue(events.stream().anyMatch(event ->
                        "agent.result".equals(event.getType())
                                && event.isLast()
                                && "stubAgent".equals(event.getNodeId())),
                "stream listener should receive a final agent result event");
    }
}
