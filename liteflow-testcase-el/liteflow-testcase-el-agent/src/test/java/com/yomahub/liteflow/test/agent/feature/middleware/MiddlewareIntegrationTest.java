package com.yomahub.liteflow.test.agent.feature.middleware;

import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import com.yomahub.liteflow.core.FlowExecutor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource("classpath:/feature/middleware/application.properties")
@SpringBootTest(classes = MiddlewareIntegrationTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.middleware")
class MiddlewareIntegrationTest {

    @Resource
    private FlowExecutor flowExecutor;

    @BeforeEach
    void reset() {
        MiddlewareAgentCmp.reset();
        FailingMiddlewareAgentCmp.reset();
    }

    @Test
    void fixedOrdersWrapTheAgentCallAsAnOnion() {
        LiteflowResponse response = flowExecutor.execute2Resp("middlewareChain", "offline");

        assertTrue(response.isSuccess());
        assertEquals(List.of("10-enter", "20-enter", "model", "20-exit", "10-exit"),
                MiddlewareAgentCmp.observations());
    }

    @Test
    void middlewareFailurePropagatesAndStopsBeforeModel() {
        LiteflowResponse response = flowExecutor.execute2Resp("middlewareFailureChain", "offline");

        assertFalse(response.isSuccess());
        assertTrue(causeMessages(response.getCause()).contains("middleware rejected"));
        assertEquals(0, FailingMiddlewareAgentCmp.modelCalls());
    }

    private static String causeMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }
}
