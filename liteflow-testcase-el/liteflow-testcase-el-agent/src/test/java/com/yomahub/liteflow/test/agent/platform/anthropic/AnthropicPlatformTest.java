package com.yomahub.liteflow.test.agent.platform.anthropic;

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
 * Anthropic 头等平台基础连通性场景测试。
 * 需要 {@code LITEFLOW_AGENT_TEST_ANTHROPIC_API_KEY}，缺失即跳过。
 */
@TestPropertySource("classpath:/platform/anthropic/application.properties")
@SpringBootTest(classes = AnthropicPlatformTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.platform.anthropic")
public class AnthropicPlatformTest extends BaseAgentLiveTest {

    private static final String PROMPT = "请用一句中文短句简要介绍 LiteFlow。";

    @BeforeEach
    public void ensureCredential() {
        LiveTestSupport.applyAnthropicOrSkip(liteflowConfig, "AnthropicPlatformTest");
    }

    @Test
    public void testAnthropicConnectivity() {
        LiteflowResponse response = flowExecutor.execute2Resp("anthropicPlatformChain", PROMPT);

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Object reply = response.getSlot().getResponseData();
        Assertions.assertNotNull(reply, "agent reply must be recorded into responseData");
        Assertions.assertFalse(reply.toString().isBlank(), "agent reply must not be blank");
    }
}
