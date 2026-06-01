package com.yomahub.liteflow.test.agent.platform.anthropiccompatible;

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

/**
 * AnthropicCompatible 网关基础连通性场景测试（自定义 baseUrl + apiKey）。
 * 需要 {@code LITEFLOW_AGENT_TEST_ANTHROPIC_GATEWAY_API_KEY} /
 * {@code LITEFLOW_AGENT_TEST_ANTHROPIC_GATEWAY_BASE_URL}，缺失即跳过。
 */
@TestPropertySource("classpath:/platform/anthropiccompatible/application.properties")
@SpringBootTest(classes = AnthropicCompatiblePlatformTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.platform.anthropiccompatible")
public class AnthropicCompatiblePlatformTest extends BaseAgentLiveTest {

    private static final String PROMPT = "请用一句中文短句简要介绍 LiteFlow。";

    @BeforeEach
    public void ensureCredential() {
        LiveTestSupport.applyAnthropicGatewayOrSkip(liteflowConfig, "AnthropicCompatiblePlatformTest");
    }

    @Test
    public void testAnthropicCompatibleConnectivity() {
        LiteflowResponse response = flowExecutor.execute2Resp("anthropiccompatiblePlatformChain", PROMPT);

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Object reply = response.getSlot().getResponseData();
        Assertions.assertNotNull(reply, "agent reply must be recorded into responseData");
        Assertions.assertFalse(reply.toString().isBlank(), "agent reply must not be blank");
    }
}
