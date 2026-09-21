package com.yomahub.liteflow.test.agent.feature.a2a;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.agent.a2a.RecordingA2aRuntimeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource("classpath:/feature/a2a/application.properties")
@SpringBootTest(classes = A2aChainTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.a2a")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class A2aChainTest {

    @Resource
    private FlowExecutor flowExecutor;

    @BeforeEach
    void resetFixture() {
        A2aAgentCmp.reset();
    }

    @Test
    void componentRuntimeIsReusedButEveryCallGetsAnIsolatedUpstreamAgent() {
        LiteflowResponse first = execute("conversation-a2a-a", "first");
        LiteflowResponse second = execute("conversation-a2a-b", "second");

        assertTrue(first.isSuccess());
        assertTrue(second.isSuccess());
        assertEquals("remote-1", first.getSlot().getResponseData());
        assertEquals("remote-2", second.getSlot().getResponseData());
        assertEquals(1, A2aAgentCmp.runtimeBuilds());
        var handles = RecordingA2aRuntimeFactory.createdHandles();
        assertEquals(2, handles.size());
        assertNotSame(handles.get(0), handles.get(1));
    }

    @Test
    void remoteErrorBecomesLiteFlowNodeFailure() {
        A2aAgentCmp.failNext(new IllegalStateException("offline remote failure"));

        LiteflowResponse response = execute("conversation-a2a-error", "error");

        assertFalse(response.isSuccess());
        assertTrue(causeMessages(response.getCause()).contains("offline remote failure"));
        assertEquals(1, RecordingA2aRuntimeFactory.createdHandles().size());
    }

    private LiteflowResponse execute(String conversation, String prompt) {
        return flowExecutor.execute2Resp(
                "a2aChain", prompt, ExecuteOption.of().conversationId(conversation));
    }

    private static String causeMessages(Throwable failure) {
        StringBuilder result = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            result.append(current.getClass().getSimpleName())
                    .append(':').append(current.getMessage()).append('\n');
        }
        return result.toString();
    }
}
