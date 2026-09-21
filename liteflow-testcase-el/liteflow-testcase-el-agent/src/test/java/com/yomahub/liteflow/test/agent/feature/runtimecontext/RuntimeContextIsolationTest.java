package com.yomahub.liteflow.test.agent.feature.runtimecontext;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.ScriptedChatModel;
import io.agentscope.core.agent.RuntimeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource("classpath:/feature/runtimecontext/application.properties")
@SpringBootTest(classes = RuntimeContextIsolationTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.runtimecontext")
public class RuntimeContextIsolationTest {

    @Resource
    private FlowExecutor flowExecutor;

    @BeforeEach
    void resetFixture() {
        RuntimeContextAgentCmp.reset();
    }

    @Test
    void singletonAgentKeepsRuntimeContextAndHistoryIsolatedByConversation() {
        LiteflowResponse first = execute("conversation-a", "remember-a");
        LiteflowResponse second = execute("conversation-b", "remember-b");
        LiteflowResponse third = execute("conversation-a", "continue-a");

        assertTrue(first.isSuccess());
        assertTrue(second.isSuccess());
        assertTrue(third.isSuccess());
        assertEquals(1, RuntimeContextAgentCmp.runtimeBuilds());

        ScriptedChatModel model = RuntimeContextAgentCmp.scriptedModel();
        RuntimeContext firstContext = model.runtimeContextAt(0);
        RuntimeContext secondContext = model.runtimeContextAt(1);
        RuntimeContext thirdContext = model.runtimeContextAt(2);
        assertNotEquals(firstContext.getSessionId(), secondContext.getSessionId());
        assertEquals(firstContext.getSessionId(), thirdContext.getSessionId());
        assertEquals(model.inputAt(0).size(), model.inputAt(1).size());
        assertTrue(model.inputAt(2).size() > model.inputAt(0).size(),
                "only conversation-a may continue its own saved history");
    }

    private LiteflowResponse execute(String conversationId, String prompt) {
        return flowExecutor.execute2Resp(
                "runtimeContextChain", prompt,
                ExecuteOption.of().conversationId(conversationId));
    }
}
