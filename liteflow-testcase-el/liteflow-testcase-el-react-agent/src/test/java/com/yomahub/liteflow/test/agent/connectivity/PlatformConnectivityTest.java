package com.yomahub.liteflow.test.agent.connectivity;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import com.yomahub.liteflow.test.agent.cmp.RecordReplyCmp;
import com.yomahub.liteflow.test.agent.connectivity.cmp.AnthropicCompatibleAgentCmp;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * 各平台真实端点的连通性冒烟测试。
 *
 * <p>每个测试方法只在用户提供对应 API Key（环境变量）时执行，
 * 否则通过 {@link Assumptions#assumeTrue} 跳过，避免 CI 与本地构建被强制依赖外网。
 *
 * <p>对应 guide §4.2 中列出的所有入口类：
 * <ul>
 *   <li>头等平台：OpenAI / Anthropic / Gemini / DashScope</li>
 *   <li>OpenAI 兼容预设：DeepSeek / Kimi / GLM / Minimax</li>
 *   <li>自定义网关：OpenAICompatible.custom / AnthropicCompatible.custom</li>
 * </ul>
 */
@TestPropertySource(value = "classpath:/agent/connectivity/application.properties")
@SpringBootTest(classes = PlatformConnectivityTest.class)
@EnableAutoConfiguration
@ComponentScan({
        "com.yomahub.liteflow.test.agent.cmp",
        "com.yomahub.liteflow.test.agent.connectivity.cmp"
})
public class PlatformConnectivityTest extends BaseAgentLiveTest {

    private static final String PROMPT = "请用一句中文短句简要介绍 LiteFlow。";

    /* ============ 头等平台 ============ */

    @Test
    public void testOpenAIConnectivity() {
        String key = LiveTestEnv.resolve(LiveTestEnv.OPENAI_API_KEY);
        Assumptions.assumeTrue(!key.isEmpty(),
                "OpenAI 未配置 " + LiveTestEnv.OPENAI_API_KEY + "，跳过");
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(key);
        String baseUrl = LiveTestEnv.resolve(LiveTestEnv.OPENAI_BASE_URL);
        if (!baseUrl.isEmpty()) {
            cred.setBaseUrl(baseUrl);
        }
        liteflowConfig.getAgent().setOpenai(cred);
        runPlatformChain("openaiChain");
    }

    @Test
    public void testAnthropicConnectivity() {
        String key = LiveTestEnv.resolve(LiveTestEnv.ANTHROPIC_API_KEY);
        Assumptions.assumeTrue(!key.isEmpty(),
                "Anthropic 未配置 " + LiveTestEnv.ANTHROPIC_API_KEY + "，跳过");
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(key);
        liteflowConfig.getAgent().setAnthropic(cred);
        runPlatformChain("anthropicChain");
    }

    @Test
    public void testGeminiConnectivity() {
        String key = LiveTestEnv.resolve(LiveTestEnv.GEMINI_API_KEY);
        Assumptions.assumeTrue(!key.isEmpty(),
                "Gemini 未配置 " + LiveTestEnv.GEMINI_API_KEY + "，跳过");
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(key);
        liteflowConfig.getAgent().setGemini(cred);
        runPlatformChain("geminiChain");
    }

    @Test
    public void testDashScopeConnectivity() {
        String key = LiveTestEnv.resolve(LiveTestEnv.DASHSCOPE_API_KEY);
        Assumptions.assumeTrue(!key.isEmpty(),
                "DashScope 未配置 " + LiveTestEnv.DASHSCOPE_API_KEY + "，跳过");
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(key);
        liteflowConfig.getAgent().setDashscope(cred);
        runPlatformChain("dashscopeChain");
    }

    /* ============ OpenAI 兼容族预设 ============ */

    @Test
    public void testDeepSeekConnectivity() {
        applyCompatibleCredentialOrSkip("deepseek", LiveTestEnv.DEEPSEEK_API_KEY, null);
        runPlatformChain("deepseekChain");
    }

    @Test
    public void testKimiConnectivity() {
        applyCompatibleCredentialOrSkip("kimi", LiveTestEnv.KIMI_API_KEY, null);
        runPlatformChain("kimiChain");
    }

    @Test
    public void testGLMConnectivity() {
        applyCompatibleCredentialOrSkip("glm", LiveTestEnv.GLM_API_KEY, null);
        runPlatformChain("glmChain");
    }

    @Test
    public void testMinimaxConnectivity() {
        applyCompatibleCredentialOrSkip("minimax", LiveTestEnv.MINIMAX_API_KEY, null);
        runPlatformChain("minimaxChain");
    }

    /* ============ 自定义网关 ============ */

    @Test
    public void testOpenAICompatibleCustomConnectivity() {
        String apiKey = LiveTestEnv.resolve(LiveTestEnv.COMPATIBLE_API_KEY);
        String baseUrl = LiveTestEnv.resolve(LiveTestEnv.COMPATIBLE_BASE_URL);
        Assumptions.assumeTrue(!apiKey.isEmpty(),
                "OpenAICompatible.custom 未配置 " + LiveTestEnv.COMPATIBLE_API_KEY + "，跳过");
        Assumptions.assumeTrue(!baseUrl.isEmpty(),
                "OpenAICompatible.custom 未配置 " + LiveTestEnv.COMPATIBLE_BASE_URL + "，跳过");
        applyCompatibleCredential(LiveTestSupport.COMPATIBLE_CONFIG_KEY, apiKey, baseUrl);
        runPlatformChain("openaiCompatibleCustomChain");
    }

    @Test
    public void testAnthropicCompatibleCustomConnectivity() {
        String apiKey = LiveTestEnv.resolve(LiveTestEnv.ANTHROPIC_GATEWAY_API_KEY);
        String baseUrl = LiveTestEnv.resolve(LiveTestEnv.ANTHROPIC_GATEWAY_BASE_URL);
        Assumptions.assumeTrue(!apiKey.isEmpty(),
                "AnthropicCompatible 未配置 " + LiveTestEnv.ANTHROPIC_GATEWAY_API_KEY + "，跳过");
        Assumptions.assumeTrue(!baseUrl.isEmpty(),
                "AnthropicCompatible 未配置 " + LiveTestEnv.ANTHROPIC_GATEWAY_BASE_URL + "，跳过");
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(apiKey);
        cred.setBaseUrl(baseUrl);
        liteflowConfig.getAgent().getAnthropicCompatible()
                .put(AnthropicCompatibleAgentCmp.GATEWAY_CONFIG_KEY, cred);
        runPlatformChain("anthropicCompatibleChain");
    }

    /* ============ 共用辅助 ============ */

    private void applyCompatibleCredentialOrSkip(String configKey, String apiKeyEnv, String baseUrl) {
        String key = LiveTestEnv.resolve(apiKeyEnv);
        Assumptions.assumeTrue(!key.isEmpty(),
                configKey + " 未配置 " + apiKeyEnv + "，跳过");
        applyCompatibleCredential(configKey, key, baseUrl);
    }

    private void applyCompatibleCredential(String configKey, String apiKey, String baseUrl) {
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(apiKey);
        if (baseUrl != null && !baseUrl.isEmpty()) {
            cred.setBaseUrl(baseUrl);
        }
        liteflowConfig.getAgent().getOpenaiCompatible().put(configKey, cred);
    }

    private void runPlatformChain(String chainId) {
        LiteflowResponse response = flowExecutor.execute2Resp(chainId, PROMPT);
        if (!response.isSuccess() && response.getCause() != null) {
            response.getCause().printStackTrace();
        }
        Assertions.assertTrue(response.isSuccess(),
                "chain " + chainId + " failed: "
                        + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Object reply = response.getSlot().getOutput(RecordReplyCmp.NODE_ID);
        Assertions.assertNotNull(reply, "agent reply must be recorded");
        Assertions.assertFalse(reply.toString().isBlank(), "agent reply must not be blank");
    }
}
