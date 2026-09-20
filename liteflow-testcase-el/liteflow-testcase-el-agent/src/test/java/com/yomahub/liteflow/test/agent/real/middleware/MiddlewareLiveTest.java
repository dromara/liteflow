package com.yomahub.liteflow.test.agent.real.middleware;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * guide §8 中间件 的真实模型验证：onAgent / onReasoning / onModelCall 切点、
 * 中间件阻断执行。
 */
@TestPropertySource("classpath:/real/middleware/application.properties")
@SpringBootTest(classes = MiddlewareLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.middleware")
public class MiddlewareLiveTest extends RealAgentTestBase {

    @BeforeEach
    public void resetTrace() {
        RealMiddlewareCmp.ObservingMiddleware.TRACE.clear();
        RealMiddlewareCmp.BlockingMiddleware.enabled = false;
    }

    /** §8：自定义中间件在真实调用的 onAgent / onReasoning / onModelCall 切点生效。 */
    @Test
    public void middlewareObservesAgentReasoningAndModelCalls() {
        LiteflowResponse response = flowExecutor.execute2Resp("realMiddlewareChain",
                "用一句话解释什么是中间件。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        var trace = RealMiddlewareCmp.ObservingMiddleware.TRACE;
        Assertions.assertTrue(trace.contains("agent:before"), "trace: " + trace);
        Assertions.assertTrue(trace.contains("agent:after"), "trace: " + trace);
        Assertions.assertTrue(trace.contains("reasoning"), "trace: " + trace);
        Assertions.assertTrue(trace.contains("model-call"), "trace: " + trace);
        Assertions.assertFalse(String.valueOf((Object) response.getSlot().getResponseData()).isBlank());
    }

    /** §8：中间件返回 Flux.error 可阻断本次调用，链执行失败。 */
    @Test
    public void blockingMiddlewareFailsTheChain() {
        RealMiddlewareCmp.BlockingMiddleware.enabled = true;
        try {
            LiteflowResponse response = flowExecutor.execute2Resp("realMiddlewareChain",
                    "你好");
            Assertions.assertFalse(response.isSuccess(),
                    "blocked middleware must fail the chain");
            Assertions.assertTrue(cause(response).contains("middleware blocked this call"),
                    "unexpected cause: " + cause(response));
        } finally {
            RealMiddlewareCmp.BlockingMiddleware.enabled = false;
        }
    }

    private static String cause(LiteflowResponse response) {
        if (response.getCause() == null) {
            return "";
        }
        StringBuilder messages = new StringBuilder();
        for (Throwable current = response.getCause();
                current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(" <- ");
        }
        return messages.toString();
    }
}
