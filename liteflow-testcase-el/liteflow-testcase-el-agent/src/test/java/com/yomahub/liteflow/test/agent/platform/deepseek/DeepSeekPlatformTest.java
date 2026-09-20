package com.yomahub.liteflow.test.agent.platform.deepseek;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * DeepSeek（OpenAI 兼容预设）基础连通性场景测试。
 * 需要 {@code LITEFLOW_AGENT_TEST_DEEPSEEK_API_KEY}，缺失即跳过。
 */
@TestPropertySource("classpath:/platform/deepseek/application.properties")
@SpringBootTest(classes = DeepSeekPlatformTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.platform.deepseek")
public class DeepSeekPlatformTest extends BaseAgentLiveTest {

    private static final String PROMPT = "请用一句中文短句简要介绍 LiteFlow。";

    @BeforeEach
    public void ensureCredential() {
        LiveTestSupport.applyCompatiblePresetOrSkip(
                liteflowConfig, "deepseek", LiveTestEnv.DEEPSEEK_API_KEY, "DeepSeekPlatformTest");
    }

    @Test
    public void testDeepSeekConnectivity() {
        LiteflowResponse response = flowExecutor.execute2Resp("deepseekPlatformChain", PROMPT);

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Object reply = response.getSlot().getResponseData();
        Assertions.assertNotNull(reply, "agent reply must be recorded into responseData");
        Assertions.assertFalse(reply.toString().isBlank(), "agent reply must not be blank");
    }
}
