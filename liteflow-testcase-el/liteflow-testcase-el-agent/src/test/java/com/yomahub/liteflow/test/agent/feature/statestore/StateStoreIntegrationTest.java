package com.yomahub.liteflow.test.agent.feature.statestore;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource("classpath:/feature/statestore/application.properties")
@SpringBootTest(classes = StateStoreIntegrationTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.statestore")
class StateStoreIntegrationTest {

    @Resource
    private FlowExecutor flowExecutor;

    @BeforeEach
    void reset() {
        StateStoreAgentCmp.reset();
        FailingStateStoreAgentCmp.reset();
    }

    @Test
    void namespacesAgentsAndContinuesOnlyTheMatchingConversation() {
        ExecuteOption conversationA = ExecuteOption.of().conversationId("state-conversation-a");
        ExecuteOption conversationB = ExecuteOption.of().conversationId("state-conversation-b");

        assertTrue(flowExecutor.execute2Resp("stateAlphaChain", "a1", conversationA).isSuccess());
        assertTrue(flowExecutor.execute2Resp("stateAlphaChain", "b1", conversationB).isSuccess());
        assertTrue(flowExecutor.execute2Resp("stateAlphaChain", "a2", conversationA).isSuccess());
        assertTrue(flowExecutor.execute2Resp("stateBetaChain", "a-beta", conversationA).isSuccess());
        assertTrue(flowExecutor.execute2Resp("stateBetaChain", "b-beta", conversationB).isSuccess());

        assertEquals(List.of(2, 2, 4), StateStoreAgentCmp.alphaMessageCounts());
        assertEquals(List.of(2, 2), StateStoreAgentCmp.betaMessageCounts());
        assertEquals(4, StateStoreAgentCmp.physicalSessions().size(),
                "two agent namespaces times two conversations must remain disjoint");
    }

    @Test
    void strictLoadFailureStopsBeforeTheModelBoundary() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "stateFailureChain", "offline",
                ExecuteOption.of().conversationId("state-load-failure"));

        assertFalse(response.isSuccess());
        assertTrue(causeMessages(response.getCause()).contains("offline state load failed"));
        assertEquals(0, FailingStateStoreAgentCmp.modelCalls());
    }

    private static String causeMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }
}
