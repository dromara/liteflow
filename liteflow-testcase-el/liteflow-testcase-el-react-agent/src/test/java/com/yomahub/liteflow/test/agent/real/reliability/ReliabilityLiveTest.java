package com.yomahub.liteflow.test.agent.real.reliability;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * guide §14 可靠性与错误处理 的真实模型验证：
 * 重试+回退模型、多模型路由、超时、并发守卫串行化。
 */
@TestPropertySource("classpath:/real/reliability/application.properties")
@SpringBootTest(classes = ReliabilityLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.reliability")
public class ReliabilityLiveTest extends RealAgentTestBase {

    @BeforeEach
    public void resetReliabilityFixture() {
        RealReliabilityAgentsCmp.reset();
        RealReliabilityAgentsCmp.LiteflowConfigHolder.set(liteflowConfig);
        liteflowConfig.getAgent().getRuntime().setTimeout(Duration.ofMinutes(3));
    }

    /** §14.1：主模型凭据无效 + maxRetries=1 → 回退到真实模型后成功。 */
    @Test
    public void fallbackModelRescuesBrokenPrimary() {
        LiteflowResponse response = flowExecutor.execute2Resp("realFallbackChain",
                "你好，请只回复：FALLBACK-OK");

        Assertions.assertTrue(response.isSuccess(),
                "fallback model must rescue the broken primary: " + cause(response));
        Object data = response.getSlot().getResponseData();
        Assertions.assertFalse(String.valueOf((Object) data).isBlank());
    }

    /** §14.2：routeModel 按请求特征选择候选模型。 */
    @Test
    public void routeModelPicksStrongModelForHardRequest() {
        LiteflowResponse hard = flowExecutor.execute2Resp("realRoutingChain",
                Map.of("type", "hard", "text", "解释一下什么是 CAP 定理。"));
        Assertions.assertTrue(hard.isSuccess(), cause(hard));
        Assertions.assertEquals("strong", RealReliabilityAgentsCmp.ROUTE_CHOICES.get(0),
                "hard request must route to the strong model");

        LiteflowResponse easy = flowExecutor.execute2Resp("realRoutingChain",
                Map.of("type", "easy", "text", "你好"));
        Assertions.assertTrue(easy.isSuccess(), cause(easy));
        Assertions.assertEquals("cheap", RealReliabilityAgentsCmp.ROUTE_CHOICES.get(1),
                "easy request must route to the cheap model");
    }

    /** §14.3 / §14.5：runtime.timeout 超时抛 AgentInvocationException(TIMEOUT)。 */
    @Test
    public void runtimeTimeoutFailsWithTimeoutInvocationException() {
        liteflowConfig.getAgent().getRuntime().setTimeout(Duration.ofMillis(1));

        LiteflowResponse response = flowExecutor.execute2Resp("realTimeoutChain",
                "你好");

        Assertions.assertFalse(response.isSuccess(), "1ms timeout must fail the chain");
        String failure = cause(response);
        Assertions.assertTrue(failure.toLowerCase().contains("timeout"),
                "failure must be a timeout, got: " + failure);
        for (Throwable current = response.getCause();
                current != null; current = current.getCause()) {
            if (current instanceof com.yomahub.liteflow.agent.exception.AgentInvocationException) {
                return;
            }
        }
        Assertions.fail("cause chain must contain AgentInvocationException, got: " + failure);
    }

    /** §14.4：同一身份并发调用被守卫串行化（观察到 max 并发 = 1）。 */
    @Test
    public void concurrentSameIdentityCallsAreSerialized() throws Exception {
        String cid = "real-guard-" + UUID.randomUUID();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<LiteflowResponse> first = pool.submit(() ->
                    flowExecutor.execute2Resp("realGuardChain", "只回复数字 1",
                            ExecuteOption.of().conversationId(cid)));
            Future<LiteflowResponse> second = pool.submit(() ->
                    flowExecutor.execute2Resp("realGuardChain", "只回复数字 2",
                            ExecuteOption.of().conversationId(cid)));

            LiteflowResponse r1 = first.get(90, TimeUnit.SECONDS);
            LiteflowResponse r2 = second.get(90, TimeUnit.SECONDS);
            Assertions.assertTrue(r1.isSuccess(), cause(r1));
            Assertions.assertTrue(r2.isSuccess(), cause(r2));
            Assertions.assertEquals(1, RealReliabilityAgentsCmp.GUARD_MAX_ACTIVE.get(),
                    "same-identity invocations must be serialized (max concurrent observed: "
                            + RealReliabilityAgentsCmp.GUARD_MAX_ACTIVE.get() + ")");
        } finally {
            pool.shutdownNow();
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
